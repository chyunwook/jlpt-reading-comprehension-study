package com.example.jlpt_study.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.jlpt_study.data.local.Converters

/**
 * 문장 데이터 모델
 * GPT가 생성하거나 로컬에 저장된 N3 레벨 문장
 */
@Entity(tableName = "sentences")
@TypeConverters(Converters::class)
data class SentenceItem(
    @PrimaryKey
    val id: String,
    val jp: String,                          // 일본어 원문
    val goldSummaryKo: String,               // 정답 요약 (한국어)
    val keywordsCore: List<String>,          // 핵심 키워드 3개
    val tags: List<String>,                  // 태그 (grammar/structure/topic)
    val blocks: List<FunctionalBlock> = emptyList(), // 기능 블록 단위 분리
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 기능 블록 - 문장 구조 파악을 위한 의미 단위
 */
data class FunctionalBlock(
    val text: String,           // 블록 텍스트 (예: "このプロジェクトは")
    val function: BlockFunction // 블록 기능
)

/**
 * 블록 기능 유형
 */
enum class BlockFunction {
    TOPIC,      // 주제/주어 (は/が)
    OBJECT,     // 목적어 (を)
    LOCATION,   // 장소/시간 (に/で/へ)
    REASON,     // 이유 (ために/ので/から)
    CONTRAST,   // 역접 (が/けれども/のに)
    CONDITION,  // 조건 (ば/たら/なら)
    CONCLUSION, // 결론 (予定だ/ことだ/つもりだ/わけだ)
    QUOTE,      // 인용 (という/ということ)
    OTHER       // 기타
}
