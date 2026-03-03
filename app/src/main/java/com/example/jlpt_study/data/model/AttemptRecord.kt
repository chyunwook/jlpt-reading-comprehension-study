package com.example.jlpt_study.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.jlpt_study.data.local.Converters

/**
 * 시도 기록 모델
 * 사용자의 각 문장 풀이 시도를 기록
 */
@Entity(tableName = "attempts")
@TypeConverters(Converters::class)
data class AttemptRecord(
    @PrimaryKey
    val id: String,
    val sentenceId: String,                  // 관련 문장 ID
    val userSummaryKo: String,               // 사용자 입력 요약
    val unknownWords: List<String>,          // 모르는 단어 목록
    val isCorrect: Boolean,                  // 정답 여부
    val matchScore: Float,                   // 매칭 점수 (0~1)
    val errorType: ErrorType,                // 오류 유형
    val responseMs: Long,                    // 응답 시간 (밀리초)
    val createdAt: Long = System.currentTimeMillis(),
    val nextReviewAt: Long? = null,          // 다음 복습 시점
    val gptResultJson: String? = null        // GPT 결과 JSON
)

/**
 * 오류 유형
 */
enum class ErrorType {
    PARTICLE,       // 조사 오류
    VERB,           // 동사 오류
    VOCAB,          // 어휘 오류
    LOGIC,          // 논리 오류
    MISSING_INFO,   // 정보 누락
    NONE            // 오류 없음
}
