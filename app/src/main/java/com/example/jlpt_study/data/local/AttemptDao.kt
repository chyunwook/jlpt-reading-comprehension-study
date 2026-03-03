package com.example.jlpt_study.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.jlpt_study.data.model.AttemptRecord
import com.example.jlpt_study.data.model.ErrorType
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attempt: AttemptRecord)

    @Update
    suspend fun update(attempt: AttemptRecord)

    @Query("SELECT * FROM attempts WHERE id = :id")
    suspend fun getById(id: String): AttemptRecord?

    @Query("SELECT * FROM attempts WHERE sentenceId = :sentenceId ORDER BY createdAt DESC")
    suspend fun getBySentenceId(sentenceId: String): List<AttemptRecord>

    @Query("SELECT * FROM attempts ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<AttemptRecord>>

    @Query("SELECT * FROM attempts WHERE isCorrect = 0 ORDER BY createdAt DESC")
    suspend fun getWrongAttempts(): List<AttemptRecord>

    @Query("SELECT * FROM attempts WHERE createdAt >= :startOfDay ORDER BY createdAt DESC")
    suspend fun getTodayAttempts(startOfDay: Long): List<AttemptRecord>

    @Query("SELECT COUNT(*) FROM attempts WHERE createdAt >= :startOfDay")
    suspend fun getTodayAttemptCount(startOfDay: Long): Int

    @Query("SELECT COUNT(*) FROM attempts WHERE createdAt >= :startOfDay AND isCorrect = 1")
    suspend fun getTodayCorrectCount(startOfDay: Long): Int

    @Query("SELECT AVG(responseMs) FROM attempts WHERE createdAt >= :startOfDay")
    suspend fun getAverageResponseTime(startOfDay: Long): Float?

    @Query("SELECT errorType, COUNT(*) as count FROM attempts WHERE isCorrect = 0 GROUP BY errorType ORDER BY count DESC LIMIT 3")
    suspend fun getTopErrorTypes(): List<ErrorTypeCount>

    @Query("SELECT * FROM attempts WHERE nextReviewAt IS NOT NULL AND nextReviewAt <= :now ORDER BY nextReviewAt ASC LIMIT :limit")
    suspend fun getDueForReview(now: Long, limit: Int): List<AttemptRecord>

    @Query("SELECT COUNT(*) FROM attempts")
    suspend fun count(): Int

    @Query("SELECT COUNT(DISTINCT DATE(createdAt / 1000, 'unixepoch', 'localtime')) FROM attempts WHERE createdAt >= :since")
    suspend fun getStudyDaysCount(since: Long): Int
}

data class ErrorTypeCount(
    val errorType: ErrorType,
    val count: Int
)
