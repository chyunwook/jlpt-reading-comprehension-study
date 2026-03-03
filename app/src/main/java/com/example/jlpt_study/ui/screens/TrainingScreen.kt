package com.example.jlpt_study.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jlpt_study.data.model.BlockFunction
import com.example.jlpt_study.data.model.FunctionalBlock
import com.example.jlpt_study.data.model.SentenceItem
import com.example.jlpt_study.ui.viewmodel.TrainingUiState

@Composable
fun TrainingScreen(
    uiState: TrainingUiState,
    onUserInputChange: (String) -> Unit,
    onToggleUnknownWord: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBackWarning by remember { mutableStateOf(false) }
    var backPressedOnce by remember { mutableStateOf(false) }

    // 뒤로가기 제한
    BackHandler {
        if (backPressedOnce) {
            onBack()
        } else {
            showBackWarning = true
            backPressedOnce = true
        }
    }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            LoadingContent(message = uiState.loadingMessage)
        } else if (uiState.currentSentence != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 상단: 진행률
                TopBar(
                    progressText = uiState.progressText,
                    isReviewMode = uiState.isReviewMode
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 일본어 문장
                SentenceDisplay(
                    sentence = uiState.currentSentence!!,
                    unknownWords = uiState.unknownWords,
                    onWordClick = onToggleUnknownWord
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 입력 필드
                OutlinedTextField(
                    value = uiState.userInput,
                    onValueChange = onUserInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text("상황을 한 줄로 요약하세요 (한국어)") 
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onSubmit()
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 모름 단어 안내
                if (uiState.unknownWords.isNotEmpty()) {
                    Text(
                        text = "모르는 단어: ${uiState.unknownWords.joinToString(", ")}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "💡 모르는 단어는 탭해서 표시하세요. 뜻 없이 계속 읽기!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                // 제출 버튼 (하단 여백 추가로 OS 네비게이션 바와 겹침 방지)
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSubmit()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "제출",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // OS 네비게이션 바와의 간격
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // 뒤로가기 경고 다이얼로그
        if (showBackWarning) {
            AlertDialog(
                onDismissRequest = { showBackWarning = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text("세션 중단") },
                text = { Text("세션 중에는 뒤로 갈 수 없습니다. 정말 나가시겠습니까?") },
                confirmButton = {
                    TextButton(onClick = onBack) {
                        Text("나가기")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackWarning = false }) {
                        Text("계속하기")
                    }
                }
            )
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopBar(
    progressText: String,
    isReviewMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 진행률
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = if (isReviewMode) "복습 $progressText" else progressText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceDisplay(
    sentence: SentenceItem,
    unknownWords: List<String>,
    onWordClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 기능 블록 단위로 표시 (기존 단어박스 스타일)
            val blocks = if (sentence.blocks.isNotEmpty()) {
                sentence.blocks
            } else {
                // fallback: 블록이 없으면 전체 문장을 하나의 블록으로
                listOf(FunctionalBlock(sentence.jp, BlockFunction.OTHER))
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                blocks.forEach { block ->
                    val isUnknown = unknownWords.contains(block.text)
                    BlockChip(
                        block = block,
                        isUnknown = isUnknown,
                        onClick = { onWordClick(block.text) }
                    )
                }
            }
            
            // 모르는 블록 안내
            if (unknownWords.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "💡 모르는 부분을 탭하면 표시됩니다",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BlockChip(
    block: FunctionalBlock,
    isUnknown: Boolean,
    onClick: () -> Unit
) {
    // 기존 단어박스 스타일 유지
    val backgroundColor = if (isUnknown) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        Color.Transparent
    }

    val borderColor = if (isUnknown) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = block.text,
                fontSize = 20.sp,
                color = if (isUnknown) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (isUnknown) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}




