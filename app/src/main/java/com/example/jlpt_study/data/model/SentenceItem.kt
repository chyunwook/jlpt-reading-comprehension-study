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
    val createdAt: Long = System.currentTimeMillis()
)
