package com.example.jlpt_study.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jlpt_study.data.model.ErrorType
import com.example.jlpt_study.ui.viewmodel.StatisticsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    uiState: StatisticsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 통계") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 연속 학습일
                StatCard(
                    title = "🔥 연속 학습일",
                    value = "${uiState.consecutiveStudyDays}일",
                    subtitle = "최근 7일 기준"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 평균 응답 시간
                StatCard(
                    title = "⏱️ 평균 응답 시간",
                    value = String.format("%.1f초", uiState.averageResponseTimeSeconds),
                    subtitle = "오늘 기준",
                    valueColor = when {
                        uiState.averageResponseTimeSeconds <= 5f -> Color(0xFF4CAF50)
                        uiState.averageResponseTimeSeconds <= 7f -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.error
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 오늘 학습 현황
                TodayProgressCard(
                    attemptCount = uiState.todayAttemptCount,
                    correctCount = uiState.todayCorrectCount,
                    accuracy = uiState.todayAccuracy
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 오답 유형 Top 3
                ErrorTypesCard(errorTypes = uiState.topErrorTypes)

                Spacer(modifier = Modifier.height(16.dp))

                // 단어장 현황
                WordBankCard(
                    totalWords = uiState.totalUnknownWords,
                    dueForReview = uiState.wordsDueForReview
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun TodayProgressCard(
    attemptCount: Int,
    correctCount: Int,
    accuracy: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "📊 오늘 학습 현황",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "시도", value = "$attemptCount")
                StatItem(label = "정답", value = "$correctCount", color = Color(0xFF4CAF50))
                StatItem(
                    label = "정답률", 
                    value = "${(accuracy * 100).toInt()}%",
                    color = when {
                        accuracy >= 0.8f -> Color(0xFF4CAF50)
                        accuracy >= 0.6f -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorTypesCard(
    errorTypes: List<com.example.jlpt_study.data.local.ErrorTypeCount>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "⚠️ 오답 유형 Top 3",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (errorTypes.isEmpty()) {
                Text(
                    text = "아직 데이터가 없습니다",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            } else {
                errorTypes.forEachIndexed { index, errorType ->
                    ErrorTypeRow(
                        rank = index + 1,
                        errorType = errorType.errorType,
                        count = errorType.count
                    )
                    if (index < errorTypes.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorTypeRow(
    rank: Int,
    errorType: ErrorType,
    count: Int
) {
    val (emoji, label) = when (errorType) {
        ErrorType.PARTICLE -> "🔤" to "조사 오류"
        ErrorType.VERB -> "🏃" to "동사 오류"
        ErrorType.VOCAB -> "📚" to "어휘 오류"
        ErrorType.LOGIC -> "🧠" to "논리 오류"
        ErrorType.MISSING_INFO -> "❓" to "정보 누락"
        ErrorType.NONE -> "✅" to "오류 없음"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$rank.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$emoji $label",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "${count}회",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WordBankCard(
    totalWords: Int,
    dueForReview: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "📝 단어장 현황",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "총 단어", value = "$totalWords")
                StatItem(
                    label = "복습 대기", 
                    value = "$dueForReview",
                    color = if (dueForReview > 0) {
                        Color(0xFFFF9800)
                    } else {
                        Color(0xFF4CAF50)
                    }
                )
            }
        }
    }
}
