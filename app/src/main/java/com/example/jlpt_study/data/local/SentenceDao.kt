package com.example.jlpt_study.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jlpt_study.data.model.SentenceItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sentence: SentenceItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sentences: List<SentenceItem>)

    @Query("SELECT * FROM sentences WHERE id = :id")
    suspend fun getById(id: String): SentenceItem?

    @Query("SELECT * FROM sentences ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<SentenceItem>>

    @Query("SELECT * FROM sentences ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<SentenceItem>

    @Query("SELECT COUNT(*) FROM sentences")
    suspend fun count(): Int

    @Query("DELETE FROM sentences WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM sentences WHERE id NOT IN (SELECT sentenceId FROM attempts WHERE isCorrect = 1) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getUnattemptedOrWrong(limit: Int): List<SentenceItem>

    @Query("SELECT * FROM sentences ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandom(limit: Int): List<SentenceItem>
}
