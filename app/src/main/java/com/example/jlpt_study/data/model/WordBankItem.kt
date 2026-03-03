package com.example.jlpt_study.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 단어장 모델
 * 사용자가 모름 표시한 단어들을 저장
 */
@Entity(tableName = "word_bank")
data class WordBankItem(
    @PrimaryKey
    val surface: String,                     // 일본어 단어
    val meaning: String = "",                // 한글 뜻
    val sentenceId: String,                  // 처음 발견된 문장 ID
    val firstSeenAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val nextReviewAt: Long = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000), // D+3
    val status: WordStatus = WordStatus.NEW
)

/**
 * 단어 학습 상태
 */
enum class WordStatus {
    NEW,        // 새로운 단어
    LEARNING,   // 학습 중
    KNOWN       // 알고 있는 단어
}
