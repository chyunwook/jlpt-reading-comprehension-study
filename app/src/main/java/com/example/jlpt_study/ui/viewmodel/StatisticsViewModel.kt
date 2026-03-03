package com.example.jlpt_study.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jlpt_study.data.local.ErrorTypeCount
import com.example.jlpt_study.data.model.WordBankItem
import com.example.jlpt_study.data.repository.JlptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val repository: JlptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val _wordBankState = MutableStateFlow(WordBankUiState())
    val wordBankState: StateFlow<WordBankUiState> = _wordBankState.asStateFlow()

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val studyDays = repository.getStudyDaysCount(7)
                val avgResponseTime = repository.getAverageResponseTime()
                val topErrors = repository.getTopErrorTypes()
                val totalWords = repository.getWordCount()
                val wordsDue = repository.getWordsDueCount()
                val todayAttempts = repository.getTodayAttemptCount()
                val todayCorrect = repository.getTodayCorrectCount()

                _uiState.value = StatisticsUiState(
                    consecutiveStudyDays = studyDays,
                    averageResponseTimeMs = avgResponseTime ?: 0f,
                    topErrorTypes = topErrors,
                    totalUnknownWords = totalWords,
                    wordsDueForReview = wordsDue,
                    todayAttemptCount = todayAttempts,
                    todayCorrectCount = todayCorrect,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun loadWordBank() {
        viewModelScope.launch {
            _wordBankState.value = _wordBankState.value.copy(isLoading = true)

            try {
                repository.getAllWordsFlow().collect { words ->
                    _wordBankState.value = WordBankUiState(
                        words = words.sortedByDescending { it.lastSeenAt },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _wordBankState.value = _wordBankState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class StatisticsUiState(
    val consecutiveStudyDays: Int = 0,
    val averageResponseTimeMs: Float = 0f,
    val topErrorTypes: List<ErrorTypeCount> = emptyList(),
    val totalUnknownWords: Int = 0,
    val wordsDueForReview: Int = 0,
    val todayAttemptCount: Int = 0,
    val todayCorrectCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val todayProgress: Float
        get() = if (todayAttemptCount > 0) {
            todayAttemptCount.toFloat() / 10f // 목표 10개
        } else 0f

    val todayAccuracy: Float
        get() = if (todayAttemptCount > 0) {
            todayCorrectCount.toFloat() / todayAttemptCount
        } else 0f

    val averageResponseTimeSeconds: Float
        get() = averageResponseTimeMs / 1000f
}

data class WordBankUiState(
    val words: List<WordBankItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class StatisticsViewModelFactory(
    private val repository: JlptRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
