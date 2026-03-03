package com.example.jlpt_study.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jlpt_study.data.model.AttemptRecord
import com.example.jlpt_study.data.model.ErrorType
import com.example.jlpt_study.data.model.SentenceItem
import com.example.jlpt_study.data.repository.JlptRepository
import com.example.jlpt_study.network.GptService
import com.example.jlpt_study.network.GradingResult
import com.example.jlpt_study.network.LocalGradingService
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
        val timerDuration = if (_uiState.value.isReviewMode) REVIEW_TIMER_SECONDS else TIMER_SECONDS
        
        _uiState.value = _uiState.value.copy(
            remainingSeconds = timerDuration,
            userInput = "",
            unknownWords = emptyList()
        )
        
        startTimer(timerDuration)
    }

    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (i in seconds downTo 0) {
                _uiState.value = _uiState.value.copy(remainingSeconds = i)
                if (i == 0) {
                    submitAnswer()
                    return@launch
                }
                delay(1000)
            }
        }
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
        timerJob?.cancel()
        val state = _uiState.value
        val currentSentence = state.sentences.getOrNull(state.currentIndex) ?: return
        
        viewModelScope.launch {
            val responseTime = System.currentTimeMillis() - sessionStartTime
            val userInput = state.userInput.ifBlank { "" }

            // 먼저 로컬 판정 수행
            val localResult = LocalGradingService.gradeLocally(
                goldSummaryKo = currentSentence.goldSummaryKo,
                userSummaryKo = userInput,
                keywordsCore = currentSentence.keywordsCore
            )

            var finalResult = GradingResultData(
                isCorrect = localResult.isCorrect,
                matchScore = localResult.matchScore,
                errorType = localResult.errorType,
                feedbackKo = localResult.feedbackKo,
                suggestedSummaryKo = currentSentence.goldSummaryKo,
                isGptResult = false
            )

            // 모르는 단어 저장
            if (state.unknownWords.isNotEmpty()) {
                repository.saveUnknownWords(state.unknownWords, currentSentence.id)
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
                gptResultJson = null
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
                correctCount = newCorrectCount
            )
        }
    }

    fun requestGptFeedback() {
        val state = _uiState.value
        val resultData = state.lastResult ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingGptFeedback = true)

            gptService?.let { service ->
                val result = service.gradeSummary(
                    jpSentence = resultData.sentence.jp,
                    goldSummaryKo = resultData.sentence.goldSummaryKo,
                    userSummaryKo = resultData.attempt.userSummaryKo,
                    unknownWords = resultData.attempt.unknownWords
                )

                result.onSuccess { gptResult ->
                    val updatedGradingResult = GradingResultData(
                        isCorrect = gptResult.isCorrect,
                        matchScore = gptResult.matchScore,
                        errorType = gptResult.errorType,
                        feedbackKo = gptResult.feedbackKo,
                        suggestedSummaryKo = gptResult.suggestedSummaryKo,
                        isGptResult = true
                    )

                    // 시도 기록 업데이트
                    val updatedAttempt = resultData.attempt.copy(
                        isCorrect = gptResult.isCorrect,
                        matchScore = gptResult.matchScore,
                        errorType = gptResult.errorType,
                        gptResultJson = gson.toJson(gptResult)
                    )
                    repository.updateAttempt(updatedAttempt)

                    _uiState.value = _uiState.value.copy(
                        lastResult = resultData.copy(
                            attempt = updatedAttempt,
                            gradingResult = updatedGradingResult
                        ),
                        isLoadingGptFeedback = false
                    )
                }.onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoadingGptFeedback = false,
                        error = "GPT 피드백 실패"
                    )
                }
            }
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
        timerJob?.cancel()
        _uiState.value = TrainingUiState()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

data class TrainingUiState(
    val sentences: List<SentenceItem> = emptyList(),
    val currentIndex: Int = 0,
    val remainingSeconds: Int = 5,
    val userInput: String = "",
    val unknownWords: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingGptFeedback: Boolean = false,
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
