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
            LoadingContent()
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
private fun LoadingContent(message: String = "GPT가 채점 중입니다...") {
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
            // 기능 블록 단위로 표시
            val blocks = if (sentence.blocks.isNotEmpty()) {
                sentence.blocks
            } else {
                // fallback: 블록이 없으면 전체 문장을 하나의 블록으로
                listOf(FunctionalBlock(sentence.jp, BlockFunction.OTHER))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
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
    // 블록 기능에 따른 색상 및 라벨
    val (backgroundColor, borderColor, functionLabel) = getBlockStyle(block.function, isUnknown)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 기능 라벨
            if (functionLabel.isNotEmpty()) {
                Text(
                    text = functionLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = borderColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = block.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isUnknown) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (isUnknown) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "❓ 모름",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 블록 기능에 따른 스타일 반환
 */
@Composable
private fun getBlockStyle(
    function: BlockFunction,
    isUnknown: Boolean
): Triple<Color, Color, String> {
    if (isUnknown) {
        return Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.error,
            getFunctionLabel(function)
        )
    }
    
    return when (function) {
        BlockFunction.TOPIC -> Triple(
            Color(0xFFE3F2FD), // Light Blue
            Color(0xFF1976D2),
            "주제/주어"
        )
        BlockFunction.REASON -> Triple(
            Color(0xFFFFF3E0), // Light Orange
            Color(0xFFF57C00),
            "이유"
        )
        BlockFunction.CONTRAST -> Triple(
            Color(0xFFF3E5F5), // Light Purple
            Color(0xFF7B1FA2),
            "역접/대조"
        )
        BlockFunction.CONDITION -> Triple(
            Color(0xFFE8F5E9), // Light Green
            Color(0xFF388E3C),
            "조건"
        )
        BlockFunction.CONCLUSION -> Triple(
            Color(0xFFFFEBEE), // Light Pink
            Color(0xFFC62828),
            "결론"
        )
        BlockFunction.QUOTE -> Triple(
            Color(0xFFFCE4EC), // Light Rose
            Color(0xFFAD1457),
            "인용"
        )
        BlockFunction.OBJECT -> Triple(
            Color(0xFFE0F7FA), // Light Cyan
            Color(0xFF00838F),
            "목적어"
        )
        BlockFunction.LOCATION -> Triple(
            Color(0xFFF1F8E9), // Light Lime
            Color(0xFF689F38),
            "장소/시간"
        )
        BlockFunction.OTHER -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            ""
        )
    }
}

private fun getFunctionLabel(function: BlockFunction): String {
    return when (function) {
        BlockFunction.TOPIC -> "주제/주어"
        BlockFunction.REASON -> "이유"
        BlockFunction.CONTRAST -> "역접/대조"
        BlockFunction.CONDITION -> "조건"
        BlockFunction.CONCLUSION -> "결론"
        BlockFunction.QUOTE -> "인용"
        BlockFunction.OBJECT -> "목적어"
        BlockFunction.LOCATION -> "장소/시간"
        BlockFunction.OTHER -> ""
    }
}


