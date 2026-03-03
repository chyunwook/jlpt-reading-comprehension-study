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

                // 제출 버튼
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
            // 문장을 단어/문자 단위로 분리하여 표시
            // 일본어는 공백이 없으므로, 문자 그룹 단위로 나눔
            val words = splitJapaneseSentence(sentence.jp)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                words.forEach { word ->
                    val isUnknown = unknownWords.contains(word)
                    WordChip(
                        word = word,
                        isUnknown = isUnknown,
                        onClick = { onWordClick(word) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WordChip(
    word: String,
    isUnknown: Boolean,
    onClick: () -> Unit
) {
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = word,
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

/**
 * 일본어 문장을 의미 있는 단위로 분리
 * 완벽한 형태소 분석은 아니지만, 탭 가능한 단위로 나눔
 */
private fun splitJapaneseSentence(sentence: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()

    // 구두점, 조사 등으로 분리
    val breakChars = setOf('、', '。', '「', '」', '（', '）', '　', ' ', '!', '?', '！', '？')
    val particles = setOf("は", "が", "を", "に", "で", "と", "も", "の", "へ", "から", "まで", "より", "など")

    for (i in sentence.indices) {
        val char = sentence[i]
        current.append(char)

        val currentStr = current.toString()
        
        // 구두점에서 분리
        if (char in breakChars) {
            if (currentStr.length > 1) {
                result.add(currentStr.dropLast(1))
                result.add(char.toString())
            } else {
                result.add(currentStr)
            }
            current = StringBuilder()
            continue
        }

        // 히라가나가 연속되면 조사일 가능성 체크
        if (current.length >= 2 && isHiragana(char)) {
            val lastTwo = currentStr.takeLast(2)
            val lastThree = if (currentStr.length >= 3) currentStr.takeLast(3) else ""
            
            if (lastThree in particles || lastTwo in particles) {
                val particleLen = if (lastThree in particles) 3 else 2
                val mainPart = currentStr.dropLast(particleLen)
                val particle = currentStr.takeLast(particleLen)
                
                if (mainPart.isNotEmpty()) {
                    result.add(mainPart)
                }
                result.add(particle)
                current = StringBuilder()
                continue
            }
        }

        // 한자에서 히라가나로 전환시 분리 (용언 활용 제외를 위해 2글자 이상 히라가나)
        if (current.length >= 4) {
            val prevChar = sentence.getOrNull(i - 1)
            if (prevChar != null && isKanji(prevChar) && isHiragana(char)) {
                // 활용어미일 수 있으므로 좀 더 진행
            } else if (isKanji(char) && current.length > 1) {
                // 한자가 나오면 이전까지를 분리
                val mainPart = currentStr.dropLast(1)
                if (mainPart.isNotEmpty()) {
                    result.add(mainPart)
                }
                current = StringBuilder()
                current.append(char)
            }
        }
    }

    if (current.isNotEmpty()) {
        result.add(current.toString())
    }

    // 빈 문자열 제거 및 너무 긴 것은 추가 분리
    return result.filter { it.isNotBlank() }.flatMap { word ->
        if (word.length > 6) {
            // 긴 단어는 3-4글자 단위로 분리
            word.chunked(4)
        } else {
            listOf(word)
        }
    }
}

private fun isHiragana(char: Char): Boolean {
    return char in '\u3040'..'\u309F'
}

private fun isKanji(char: Char): Boolean {
    return char in '\u4E00'..'\u9FAF'
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
