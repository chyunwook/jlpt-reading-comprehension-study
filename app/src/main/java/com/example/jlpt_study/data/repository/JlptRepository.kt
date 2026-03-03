package com.example.jlpt_study.data.repository

import com.example.jlpt_study.data.local.AttemptDao
import com.example.jlpt_study.data.local.ErrorTypeCount
import com.example.jlpt_study.data.local.SentenceDao
import com.example.jlpt_study.data.local.WordBankDao
import com.example.jlpt_study.data.model.AttemptRecord
import com.example.jlpt_study.data.model.SentenceItem
import com.example.jlpt_study.data.model.WordBankItem
import com.example.jlpt_study.data.model.WordStatus
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class JlptRepository(
    private val sentenceDao: SentenceDao,
    private val attemptDao: AttemptDao,
    private val wordBankDao: WordBankDao
) {
    // Sentence operations
    suspend fun insertSentence(sentence: SentenceItem) = sentenceDao.insert(sentence)
    suspend fun insertSentences(sentences: List<SentenceItem>) = sentenceDao.insertAll(sentences)
    suspend fun getSentenceById(id: String) = sentenceDao.getById(id)
    fun getAllSentencesFlow(): Flow<List<SentenceItem>> = sentenceDao.getAllFlow()
    suspend fun getSentenceCount() = sentenceDao.count()
    suspend fun getRandomSentences(limit: Int) = sentenceDao.getRandom(limit)
    suspend fun getTrainingSentences(limit: Int): List<SentenceItem> {
        val unattempted = sentenceDao.getUnattemptedOrWrong(limit)
        return if (unattempted.size < limit) {
            unattempted + sentenceDao.getRandom(limit - unattempted.size)
        } else {
            unattempted
        }
    }

    // Attempt operations
    suspend fun insertAttempt(attempt: AttemptRecord) = attemptDao.insert(attempt)
    suspend fun updateAttempt(attempt: AttemptRecord) = attemptDao.update(attempt)
    suspend fun getAttemptById(id: String) = attemptDao.getById(id)
    suspend fun getAttemptsBySentenceId(sentenceId: String) = attemptDao.getBySentenceId(sentenceId)
    fun getAllAttemptsFlow(): Flow<List<AttemptRecord>> = attemptDao.getAllFlow()
    suspend fun getWrongAttempts() = attemptDao.getWrongAttempts()
    
    suspend fun getTodayAttempts(): List<AttemptRecord> {
        val startOfDay = getStartOfDay()
        return attemptDao.getTodayAttempts(startOfDay)
    }
    
    suspend fun getTodayAttemptCount(): Int {
        val startOfDay = getStartOfDay()
        return attemptDao.getTodayAttemptCount(startOfDay)
    }
    
    suspend fun getTodayCorrectCount(): Int {
        val startOfDay = getStartOfDay()
        return attemptDao.getTodayCorrectCount(startOfDay)
    }
    
    suspend fun getAverageResponseTime(): Float? {
        val startOfDay = getStartOfDay()
        return attemptDao.getAverageResponseTime(startOfDay)
    }
    
    suspend fun getTopErrorTypes(): List<ErrorTypeCount> = attemptDao.getTopErrorTypes()
    
    suspend fun getDueForReview(limit: Int): List<AttemptRecord> {
        return attemptDao.getDueForReview(System.currentTimeMillis(), limit)
    }

    suspend fun getStudyDaysCount(days: Int): Int {
        val since = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return attemptDao.getStudyDaysCount(since)
    }

    // Word bank operations
    suspend fun insertWord(word: WordBankItem) = wordBankDao.insert(word)
    suspend fun insertWords(words: List<WordBankItem>) = wordBankDao.insertAll(words)
    suspend fun getWordBySurface(surface: String) = wordBankDao.getBySurface(surface)
    fun getAllWordsFlow(): Flow<List<WordBankItem>> = wordBankDao.getAllFlow()
    suspend fun getUnknownWords() = wordBankDao.getUnknownWords()
    suspend fun getWordCount() = wordBankDao.count()
    suspend fun getWordCountByStatus(status: WordStatus) = wordBankDao.countByStatus(status)
    suspend fun getWordsDueForReview(limit: Int) = wordBankDao.getDueForReview(System.currentTimeMillis(), limit)
    suspend fun getWordsDueCount() = wordBankDao.countDueForReview(System.currentTimeMillis())
    
    suspend fun updateWordStatus(surface: String, status: WordStatus) {
        val nextReviewAt = when (status) {
            WordStatus.NEW -> System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L) // D+3
            WordStatus.LEARNING -> System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L) // D+7
            WordStatus.KNOWN -> Long.MAX_VALUE // No more review
        }
        wordBankDao.updateStatus(surface, status, nextReviewAt)
    }

    suspend fun saveUnknownWords(words: List<String>, sentenceId: String) {
        val wordItems = words.map { word ->
            WordBankItem(
                surface = word,
                sentenceId = sentenceId,
                firstSeenAt = System.currentTimeMillis(),
                lastSeenAt = System.currentTimeMillis(),
                nextReviewAt = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L),
                status = WordStatus.NEW
            )
        }
        wordBankDao.insertAll(wordItems)
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    // SRS 복습 규칙 적용
    fun calculateNextReviewAt(attempt: AttemptRecord, consecutiveCorrect: Int): Long {
        return if (attempt.isCorrect) {
            when (consecutiveCorrect) {
                0 -> System.currentTimeMillis() + (1 * 24 * 60 * 60 * 1000L) // D+1
                1 -> System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L) // D+3
                else -> System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L) // D+7
            }
        } else {
            System.currentTimeMillis() + (1 * 24 * 60 * 60 * 1000L) // D+1 for wrong answers
        }
    }
}
