package com.example.jlpt_study.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.jlpt_study.data.model.AttemptRecord
import com.example.jlpt_study.data.model.SentenceItem
import com.example.jlpt_study.data.model.WordBankItem

@Database(
    entities = [SentenceItem::class, AttemptRecord::class, WordBankItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sentenceDao(): SentenceDao
    abstract fun attemptDao(): AttemptDao
    abstract fun wordBankDao(): WordBankDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jlpt_study_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
