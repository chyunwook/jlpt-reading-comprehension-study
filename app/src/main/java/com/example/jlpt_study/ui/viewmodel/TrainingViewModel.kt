package com.example.jlpt_study.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jlpt_study.data.model.AttemptRecord
import com.example.jlpt_study.data.model.ErrorType
import com.example.jlpt_study.data.model.SentenceItem
import com.example.jlpt_study.data.repository.JlptRepository
import com.example.jlpt_study.network.GptService
import com.example.jlpt_study.network.LocalGradingService
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class TrainingViewModel(
    private val repository: JlptRepository,
    private val gptService: GptService?
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private var sessionStartTime: Long = 0L
    private val gson = Gson()

    companion object {
        const val SESSION_SIZE = 10
    }

    fun startSession(isReview: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                isReviewMode = isReview
            )

            val sentences = if (isReview) {
                getReviewSentences()
            } else {
                repository.getTrainingSentences(SESSION_SIZE)
            }

            if (sentences.isEmpty()) {
                // 문장이 없으면 생성 시도
                generateNewSentences()
            } else {
                _uiState.value = _uiState.value.copy(
                    sentences = sentences,
                    currentIndex = 0,
                    isLoading = false
                )
                startCurrentSentence()
            }
        }
    }

    private suspend fun getReviewSentences(): List<SentenceItem> {
        val dueAttempts = repository.getDueForReview(SESSION_SIZE)
        return dueAttempts.mapNotNull { attempt ->
            repository.getSentenceById(attempt.sentenceId)
        }
    }

    fun generateNewSentences() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            gptService?.let { service ->
                val result = service.generateSentences(SESSION_SIZE)
                result.onSuccess { sentences ->
                    repository.insertSentences(sentences)
                    _uiState.value = _uiState.value.copy(
                        sentences = sentences,
                        currentIndex = 0,
                        isLoading = false
                    )
                    startCurrentSentence()
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "문장 생성 실패: ${e.message}"
                    )
                }
            } ?: run {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "API 키가 설정되지 않았습니다."
                )
            }
        }
    }

    private fun startCurrentSentence() {
        sessionStartTime = System.currentTimeMillis()
        
        _uiState.value = _uiState.value.copy(
            userInput = "",
            unknownWords = emptyList()
        )
    }

    fun updateUserInput(input: String) {
        _uiState.value = _uiState.value.copy(userInput = input)
    }

    fun toggleUnknownWord(word: String) {
        val currentWords = _uiState.value.unknownWords.toMutableList()
        if (currentWords.contains(word)) {
            currentWords.remove(word)
        } else {
            currentWords.add(word)
        }
        _uiState.value = _uiState.value.copy(unknownWords = currentWords)
    }

    fun submitAnswer() {
        val state = _uiState.value
        val currentSentence = state.sentences.getOrNull(state.currentIndex) ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val responseTime = System.currentTimeMillis() - sessionStartTime
            val userInput = state.userInput.ifBlank { "" }

            // 모르는 단어 저장
            if (state.unknownWords.isNotEmpty()) {
                repository.saveUnknownWords(state.unknownWords, currentSentence.id)
            }

            // GPT로 채점 (100% GPT)
            var finalResult: GradingResultData
            var gptResultJson: String? = null

            if (gptService != null && userInput.isNotBlank()) {
                val gptResult = gptService.gradeSummary(
                    jpSentence = currentSentence.jp,
                    goldSummaryKo = currentSentence.goldSummaryKo,
                    userSummaryKo = userInput,
                    unknownWords = state.unknownWords
                )

                gptResult.onSuccess { result ->
                    finalResult = GradingResultData(
                        isCorrect = result.isCorrect,
                        matchScore = result.matchScore,
                        errorType = result.errorType,
                        feedbackKo = result.feedbackKo,
                        suggestedSummaryKo = result.suggestedSummaryKo,
                        isGptResult = true
                    )
                    gptResultJson = gson.toJson(result)
                }.onFailure { e ->
                    // GPT 실패 시 로컬 fallback
                    val localResult = LocalGradingService.gradeLocally(
                        goldSummaryKo = currentSentence.goldSummaryKo,
                        userSummaryKo = userInput,
                        keywordsCore = currentSentence.keywordsCore
                    )
                    finalResult = GradingResultData(
                        isCorrect = localResult.isCorrect,
                        matchScore = localResult.matchScore,
                        errorType = localResult.errorType,
                        feedbackKo = "${localResult.feedbackKo} (GPT 오류: ${e.message})",
                        suggestedSummaryKo = currentSentence.goldSummaryKo,
                        isGptResult = false
                    )
                }

                finalResult = gptResult.getOrNull()?.let { result ->
                    GradingResultData(
                        isCorrect = result.isCorrect,
                        matchScore = result.matchScore,
                        errorType = result.errorType,
                        feedbackKo = result.feedbackKo,
                        suggestedSummaryKo = result.suggestedSummaryKo,
                        isGptResult = true
                    )
                } ?: run {
                    val localResult = LocalGradingService.gradeLocally(
                        goldSummaryKo = currentSentence.goldSummaryKo,
                        userSummaryKo = userInput,
                        keywordsCore = currentSentence.keywordsCore
                    )
                    GradingResultData(
                        isCorrect = localResult.isCorrect,
                        matchScore = localResult.matchScore,
                        errorType = localResult.errorType,
                        feedbackKo = "${localResult.feedbackKo} (GPT 연결 실패)",
                        suggestedSummaryKo = currentSentence.goldSummaryKo,
                        isGptResult = false
                    )
                }
            } else if (userInput.isBlank()) {
                // 빈 입력
                finalResult = GradingResultData(
                    isCorrect = false,
                    matchScore = 0f,
                    errorType = ErrorType.MISSING_INFO,
                    feedbackKo = "답변을 입력해주세요.",
                    suggestedSummaryKo = currentSentence.goldSummaryKo,
                    isGptResult = false
                )
            } else {
                // API 키 없음
                finalResult = GradingResultData(
                    isCorrect = false,
                    matchScore = 0f,
                    errorType = ErrorType.NONE,
                    feedbackKo = "API 키가 설정되지 않았습니다. 설정에서 OpenAI API 키를 입력해주세요.",
                    suggestedSummaryKo = currentSentence.goldSummaryKo,
                    isGptResult = false
                )
            }

            // 시도 기록 저장
            val attempt = AttemptRecord(
                id = UUID.randomUUID().toString(),
                sentenceId = currentSentence.id,
                userSummaryKo = userInput,
                unknownWords = state.unknownWords,
                isCorrect = finalResult.isCorrect,
                matchScore = finalResult.matchScore,
                errorType = finalResult.errorType,
                responseMs = responseTime,
                createdAt = System.currentTimeMillis(),
                nextReviewAt = if (!finalResult.isCorrect) {
                    System.currentTimeMillis() + (24 * 60 * 60 * 1000) // D+1
                } else null,
                gptResultJson = gptResultJson
            )
            repository.insertAttempt(attempt)

            val newCorrectCount = if (finalResult.isCorrect) {
                _uiState.value.correctCount + 1
            } else {
                _uiState.value.correctCount
            }

            _uiState.value = _uiState.value.copy(
                lastResult = ResultData(
                    sentence = currentSentence,
                    attempt = attempt,
                    gradingResult = finalResult
                ),
                showResult = true,
                correctCount = newCorrectCount,
                isLoading = false
            )
        }
    }

    fun nextSentence() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1

        if (nextIndex >= state.sentences.size) {
            _uiState.value = _uiState.value.copy(
                isSessionComplete = true,
                showResult = false
            )
        } else {
            _uiState.value = _uiState.value.copy(
                currentIndex = nextIndex,
                showResult = false,
                lastResult = null
            )
            startCurrentSentence()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetSession() {
        _uiState.value = TrainingUiState()
    }
}

data class TrainingUiState(
    val sentences: List<SentenceItem> = emptyList(),
    val currentIndex: Int = 0,
    val userInput: String = "",
    val unknownWords: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val showResult: Boolean = false,
    val lastResult: ResultData? = null,
    val isSessionComplete: Boolean = false,
    val isReviewMode: Boolean = false,
    val error: String? = null,
    val correctCount: Int = 0  // 정답 수 추적
) {
    val currentSentence: SentenceItem?
        get() = sentences.getOrNull(currentIndex)
    
    val progress: Float
        get() = if (sentences.isNotEmpty()) {
            (currentIndex + 1).toFloat() / sentences.size
        } else 0f
    
    val progressText: String
        get() = "${currentIndex + 1}/${sentences.size}"
}

data class ResultData(
    val sentence: SentenceItem,
    val attempt: AttemptRecord,
    val gradingResult: GradingResultData
)

data class GradingResultData(
    val isCorrect: Boolean,
    val matchScore: Float,
    val errorType: ErrorType,
    val feedbackKo: String,
    val suggestedSummaryKo: String,
    val isGptResult: Boolean
)

class TrainingViewModelFactory(
    private val repository: JlptRepository,
    private val gptService: GptService?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrainingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrainingViewModel(repository, gptService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
