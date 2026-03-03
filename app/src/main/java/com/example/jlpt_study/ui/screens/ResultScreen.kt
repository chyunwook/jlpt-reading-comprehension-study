package com.example.jlpt_study.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jlpt_study.data.model.SentenceItem
import com.example.jlpt_study.ui.viewmodel.GradingResultData
import com.example.jlpt_study.ui.viewmodel.ResultData

@Composable
fun ResultScreen(
    resultData: ResultData,
    isLoadingGptFeedback: Boolean,
    onRequestGptFeedback: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isCorrect = resultData.gradingResult.isCorrect

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 정답/오답 아이콘
        ResultIcon(isCorrect = isCorrect)

        Spacer(modifier = Modifier.height(16.dp))

        // 정답/오답 텍스트
        Text(
            text = if (isCorrect) "정답!" else "다시 확인해보세요",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isCorrect) {
                Color(0xFF4CAF50)
            } else {
                MaterialTheme.colorScheme.error
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 원문 카드
        SentenceCard(
            title = "일본어 원문",
            content = resultData.sentence.jp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 정답 요약 카드
        SentenceCard(
            title = "정답 요약",
            content = resultData.gradingResult.suggestedSummaryKo,
            highlight = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 내 답안 카드
        if (resultData.attempt.userSummaryKo.isNotBlank()) {
            SentenceCard(
                title = "내 답안",
                content = resultData.attempt.userSummaryKo,
                isError = !isCorrect
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 피드백 카드
        FeedbackCard(
            feedback = resultData.gradingResult.feedbackKo,
            isGptResult = resultData.gradingResult.isGptResult
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 핵심 키워드
        KeywordsSection(keywords = resultData.sentence.keywordsCore)

        Spacer(modifier = Modifier.height(16.dp))

        // 모르는 단어
        if (resultData.attempt.unknownWords.isNotEmpty()) {
            UnknownWordsSection(words = resultData.attempt.unknownWords)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // GPT 피드백 요청 버튼 (아직 GPT 결과가 아닐 때만 표시)
        if (!resultData.gradingResult.isGptResult) {
            OutlinedButton(
                onClick = onRequestGptFeedback,
                enabled = !isLoadingGptFeedback,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoadingGptFeedback) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isLoadingGptFeedback) "피드백 받는 중..." else "GPT 피드백 받기"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // 다음 문제 버튼
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "다음 문제",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ResultIcon(isCorrect: Boolean) {
    val backgroundColor = if (isCorrect) {
        Color(0xFF4CAF50)
    } else {
        MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        color = backgroundColor.copy(alpha = 0.15f)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = backgroundColor
            )
        }
    }
}

@Composable
private fun SentenceCard(
    title: String,
    content: String,
    highlight: Boolean = false,
    isError: Boolean = false
) {
    val backgroundColor = when {
        highlight -> MaterialTheme.colorScheme.primaryContainer
        isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 18.sp,
                fontWeight = if (highlight) FontWeight.Medium else FontWeight.Normal,
                color = when {
                    highlight -> MaterialTheme.colorScheme.onPrimaryContainer
                    isError -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    feedback: String,
    isGptResult: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💬 피드백",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (isGptResult) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiary
                    ) {
                        Text(
                            text = "GPT",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feedback,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun KeywordsSection(keywords: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "핵심 키워드",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keywords.forEach { keyword ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = keyword,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun UnknownWordsSection(words: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "📝 모름 표시한 단어 (단어장에 저장됨)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            words.forEach { word ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = word,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
