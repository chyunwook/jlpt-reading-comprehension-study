package com.example.jlpt_study.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.jlpt_study.data.model.WordBankItem
import com.example.jlpt_study.data.model.WordStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WordBankDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: WordBankItem)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<WordBankItem>)

    @Update
    suspend fun update(word: WordBankItem)

    @Query("SELECT * FROM word_bank WHERE surface = :surface")
    suspend fun getBySurface(surface: String): WordBankItem?

    @Query("SELECT * FROM word_bank ORDER BY lastSeenAt DESC")
    fun getAllFlow(): Flow<List<WordBankItem>>

    @Query("SELECT * FROM word_bank WHERE status != 'KNOWN' ORDER BY lastSeenAt DESC")
    suspend fun getUnknownWords(): List<WordBankItem>

    @Query("SELECT * FROM word_bank WHERE nextReviewAt <= :now ORDER BY nextReviewAt ASC LIMIT :limit")
    suspend fun getDueForReview(now: Long, limit: Int): List<WordBankItem>

    @Query("SELECT COUNT(*) FROM word_bank")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM word_bank WHERE status = :status")
    suspend fun countByStatus(status: WordStatus): Int

    @Query("SELECT COUNT(*) FROM word_bank WHERE nextReviewAt <= :now")
    suspend fun countDueForReview(now: Long): Int

    @Query("UPDATE word_bank SET lastSeenAt = :now WHERE surface = :surface")
    suspend fun updateLastSeen(surface: String, now: Long)

    @Query("UPDATE word_bank SET status = :status, nextReviewAt = :nextReviewAt WHERE surface = :surface")
    suspend fun updateStatus(surface: String, status: WordStatus, nextReviewAt: Long)
}
