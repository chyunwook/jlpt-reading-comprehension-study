package com.example.jlpt_study.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SessionCompleteScreen(
    totalQuestions: Int,
    correctAnswers: Int,
    onGoHome: () -> Unit,
    onStartReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accuracy = if (totalQuestions > 0) {
        (correctAnswers.toFloat() / totalQuestions * 100).toInt()
    } else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 축하 아이콘
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = Color(0xFFFFD700).copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color(0xFFFFD700)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "세션 완료!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "오늘도 수고하셨습니다 💪",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 결과 카드
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ResultStat(
                        label = "총 문제",
                        value = "$totalQuestions"
                    )
                    ResultStat(
                        label = "정답",
                        value = "$correctAnswers",
                        color = Color(0xFF4CAF50)
                    )
                    ResultStat(
                        label = "정답률",
                        value = "$accuracy%",
                        color = when {
                            accuracy >= 80 -> Color(0xFF4CAF50)
                            accuracy >= 60 -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 격려 메시지
        val encouragement = when {
            accuracy >= 90 -> "완벽해요! 🎉"
            accuracy >= 70 -> "잘하고 있어요! 👍"
            accuracy >= 50 -> "조금만 더 힘내요! 💪"
            else -> "복습으로 실력을 키워봐요! 📚"
        }

        Text(
            text = encouragement,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 버튼들
        Button(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "홈으로",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onStartReview,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "바로 복습하기",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ResultStat(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
