package com.example.jlpt_study.network

import com.example.jlpt_study.data.model.ErrorType

/**
 * 로컬 판정 서비스
 * GPT 호출 실패 시 fallback으로 사용
 */
object LocalGradingService {
    
    /**
     * 키워드 기반 간단 판정
     */
    fun gradeLocally(
        goldSummaryKo: String,
        userSummaryKo: String,
        keywordsCore: List<String>
    ): LocalGradingResult {
        if (userSummaryKo.isBlank()) {
            return LocalGradingResult(
                isCorrect = false,
                matchScore = 0f,
                errorType = ErrorType.MISSING_INFO,
                feedbackKo = "답변을 입력해주세요."
            )
        }

        // 키워드 매칭 점수 계산
        val matchedKeywords = keywordsCore.count { keyword ->
            userSummaryKo.contains(keyword) || 
            goldSummaryKo.contains(keyword) && userSummaryKo.length > 5
        }
        
        val keywordScore = if (keywordsCore.isNotEmpty()) {
            matchedKeywords.toFloat() / keywordsCore.size
        } else {
            0.5f
        }

        // 정답 요약과의 유사도 (간단한 문자열 비교)
        val goldWords = goldSummaryKo.split(" ", ".", ",", "을", "를", "이", "가", "은", "는")
            .filter { it.length >= 2 }
        val userWords = userSummaryKo.split(" ", ".", ",", "을", "를", "이", "가", "은", "는")
            .filter { it.length >= 2 }
        
        val matchedWords = goldWords.count { word -> 
            userWords.any { it.contains(word) || word.contains(it) }
        }
        val wordScore = if (goldWords.isNotEmpty()) {
            matchedWords.toFloat() / goldWords.size
        } else {
            0.5f
        }

        val totalScore = (keywordScore * 0.6f + wordScore * 0.4f).coerceIn(0f, 1f)
        val isCorrect = totalScore >= 0.5f

        val errorType = when {
            isCorrect -> ErrorType.NONE
            totalScore < 0.2f -> ErrorType.MISSING_INFO
            matchedKeywords == 0 -> ErrorType.VOCAB
            else -> ErrorType.LOGIC
        }

        val feedbackKo = when {
            isCorrect -> "잘 이해했습니다!"
            totalScore >= 0.4f -> "거의 맞았어요. 핵심 정보를 조금 더 정확히 파악해보세요."
            totalScore >= 0.2f -> "핵심 키워드가 빠졌어요. 문장 구조를 다시 확인해보세요."
            else -> "문장의 핵심 내용을 다시 확인해보세요."
        }

        return LocalGradingResult(
            isCorrect = isCorrect,
            matchScore = totalScore,
            errorType = errorType,
            feedbackKo = feedbackKo
        )
    }
}

data class LocalGradingResult(
    val isCorrect: Boolean,
    val matchScore: Float,
    val errorType: ErrorType,
    val feedbackKo: String
)
