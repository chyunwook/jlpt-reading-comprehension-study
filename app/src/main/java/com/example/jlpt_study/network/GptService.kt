package com.example.jlpt_study.network

import android.util.Log
import com.example.jlpt_study.data.model.BlockFunction
import com.example.jlpt_study.data.model.ErrorType
import com.example.jlpt_study.data.model.FunctionalBlock
import com.example.jlpt_study.data.model.SentenceItem
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "GptService"

class GptService(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val baseUrl = "https://api.openai.com/v1/chat/completions"

    /**
     * N3 레벨 문장 생성 (속독 훈련용 + 기능 블록 분리)
     */
    suspend fun generateSentences(count: Int = 10): Result<List<SentenceItem>> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """You are a JLPT N3 reading comprehension item writer.

Your task is to generate JLPT N3-level reading training sentences
for speed-reading practice (not casual conversation).

The purpose is NOT translation practice.
The purpose is to train learners to:
- Ignore unknown words
- Focus on particles and verbs
- Identify cause, contrast, and conclusion structure quickly"""

            val userPrompt = """Generate $count sentences.

========================
DIFFICULTY REQUIREMENTS
========================
- Must match actual JLPT N3 読解 difficulty
- Length: 50–100 Japanese characters
- Use 2–3 of these grammar patterns per sentence:

Cause:
ため(に), ので, ことから, せいで, おかげで, によって

Contrast:
が, けれども, のに, にもかかわらず, 一方(で)

Hearsay / Quote:
ということだ, とのことだ, そうだ

Conclusion / Conjecture:
らしい, ようだ, はずだ, わけだ, 予定だ, ことになる

Context should resemble:
- announcements
- workplace situations
- public information
- daily-life notices
NOT simple dialogue.

Avoid rare N2/N1 vocabulary.

========================
FUNCTIONAL BLOCK SPLIT
========================
Split each sentence into clause-level meaning blocks
for structural reading training.

IMPORTANT:
- Do NOT split into individual words.
- 3 to 6 blocks per sentence.
- Each block should be a meaningful clause.
- Blocks must connect exactly to recreate the original sentence.

Use ONLY these block types:
TOPIC, REASON, CONTRAST, QUOTE, CONCLUSION, OTHER

========================
KOREAN SUMMARY RULES (중요!)
========================
gold_summary_ko는 일본어 어순을 따라서 작성.
- 독해 훈련용이므로 일본어 문장 순서대로 번역.
- 앞에서부터 읽으면서 이해할 수 있게.
- 25자 내외.

예시 (일본어 어순 유지):
JP: "このプロジェクトは、予算が足りないために、遅れている"
순서: 이 프로젝트는 → 예산이 부족해서 → 늦어지고 있다
정답: "이 프로젝트는 예산 부족으로 지연 중이다"

JP: "会議が延期される予定だということだ"
순서: 회의가 → 연기될 예정이라고 한다
정답: "회의가 연기될 예정이라고 한다"

========================
OUTPUT FORMAT
========================
Return ONLY valid JSON.
No markdown.
No explanations.
No code fences.

{
  "items": [
    {
      "jp": "Full Japanese sentence",
      "blocks": [
        {"text": "...", "function": "TOPIC"},
        {"text": "...", "function": "REASON"},
        {"text": "...", "function": "CONCLUSION"}
      ],
      "gold_summary_ko": "일본어 어순을 따른 한국어 요약 (25자 내외)",
      "keywords_core": ["JapaneseWord1", "JapaneseWord2", "JapaneseWord3"],
      "tags": ["grammar:ため", "structure:cause_result"]
    }
  ]
}

If you cannot follow these instructions exactly, return: {"error":"cannot_comply"}"""

            val response = callGpt(systemPrompt, userPrompt)
            Log.d(TAG, "========== GPT 문장 생성 응답 ==========")
            Log.d(TAG, "Raw response: $response")
            
            val parsed = gson.fromJson(response, GeneratedSentencesResponse::class.java)
            
            // 각 문장의 원문과 정답 요약 로그 출력
            parsed.items.forEachIndexed { index, item ->
                Log.d(TAG, "--- 문장 ${index + 1} ---")
                Log.d(TAG, "JP: ${item.jp}")
                Log.d(TAG, "정답요약: ${item.goldSummaryKo}")
                Log.d(TAG, "블록: ${item.blocks?.map { "${it.function}: ${it.text}" }}")
            }
            Log.d(TAG, "========================================")
            
            val sentences = parsed.items.map { item ->
                val blocks = item.blocks?.map { block ->
                    FunctionalBlock(
                        text = block.text,
                        function = try {
                            BlockFunction.valueOf(block.function.uppercase())
                        } catch (e: Exception) {
                            BlockFunction.OTHER
                        }
                    )
                } ?: emptyList()
                
                SentenceItem(
                    id = UUID.randomUUID().toString(),
                    jp = item.jp,
                    goldSummaryKo = item.goldSummaryKo,
                    keywordsCore = item.keywordsCore,
                    tags = item.tags,
                    blocks = blocks,
                    createdAt = System.currentTimeMillis()
                )
            }
            Result.success(sentences)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 사용자 답안 채점/피드백
     */
    suspend fun gradeSummary(
        jpSentence: String,
        goldSummaryKo: String,
        userSummaryKo: String,
        unknownWords: List<String>
    ): Result<GradingResult> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """You grade JLPT N3 reading comprehension answers.
Return ONLY valid JSON. No explanations."""

            val userPrompt = """일본어: "$jpSentence"
정답: "$goldSummaryKo"
유저답안: "$userSummaryKo"

=== 채점 기준 (엄격하게) ===
1. 문장을 끝까지 완성해야 정답
2. 일본어 어순대로 번역해야 정답
3. 핵심 요소 (주어/대조/결론) 모두 있어야 정답

오답 예시:
- 문장 미완성 → missing_info
- 어순 틀림 → logic
- 주어/목적어 틀림 → particle
- 동사 의미 틀림 → verb

=== JSON 출력 ===
{
  "is_correct": true/false,
  "match_score": 0.0-1.0,
  "error_type": "none|particle|verb|vocab|logic|missing_info",
  "one_line_feedback_ko": "",
  "suggested_summary_ko": "$goldSummaryKo",
  "core_structure": {"cause": "", "result": "", "contrast": ""},
  "keywords_core": [],
  "words_optional": []
}"""

            val response = callGpt(systemPrompt, userPrompt)
            val parsed = gson.fromJson(response, GptGradingResponse::class.java)
            
            val result = GradingResult(
                isCorrect = parsed.isCorrect,
                matchScore = parsed.matchScore,
                errorType = try {
                    ErrorType.valueOf(parsed.errorType.uppercase())
                } catch (e: Exception) {
                    ErrorType.NONE
                },
                feedbackKo = parsed.oneLineFeedbackKo,
                suggestedSummaryKo = parsed.suggestedSummaryKo,
                coreStructure = parsed.coreStructure,
                keywordsCore = parsed.keywordsCore,
                wordsOptional = parsed.wordsOptional
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callGpt(systemPrompt: String, userPrompt: String): String {
        val requestBody = GptRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                Message("system", systemPrompt),
                Message("user", userPrompt)
            ),
            temperature = 0.7,
            maxTokens = 2000
        )

        val jsonBody = gson.toJson(requestBody)
        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("GPT API error: ${response.code} ${response.message}")
        }

        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val gptResponse = gson.fromJson(responseBody, GptResponse::class.java)
        
        return gptResponse.choices.firstOrNull()?.message?.content
            ?: throw Exception("No content in response")
    }
}

// Request/Response models
data class GptRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
    @SerializedName("max_tokens") val maxTokens: Int
)

data class Message(
    val role: String,
    val content: String
)

data class GptResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: MessageContent
)

data class MessageContent(
    val content: String
)

// Sentence generation response
data class GeneratedSentencesResponse(
    val items: List<GeneratedSentenceItem>
)

data class GeneratedSentenceItem(
    val jp: String,
    val blocks: List<GeneratedBlock>?,
    @SerializedName("gold_summary_ko") val goldSummaryKo: String,
    @SerializedName("keywords_core") val keywordsCore: List<String>,
    val tags: List<String>
)

data class GeneratedBlock(
    val text: String,
    val function: String
)

// Grading response
data class GptGradingResponse(
    @SerializedName("is_correct") val isCorrect: Boolean,
    @SerializedName("match_score") val matchScore: Float,
    @SerializedName("error_type") val errorType: String,
    @SerializedName("one_line_feedback_ko") val oneLineFeedbackKo: String,
    @SerializedName("suggested_summary_ko") val suggestedSummaryKo: String,
    @SerializedName("core_structure") val coreStructure: CoreStructure,
    @SerializedName("keywords_core") val keywordsCore: List<String>,
    @SerializedName("words_optional") val wordsOptional: List<String>
)

data class CoreStructure(
    val cause: String?,
    val result: String?,
    val contrast: String?
)

// Grading result model
data class GradingResult(
    val isCorrect: Boolean,
    val matchScore: Float,
    val errorType: ErrorType,
    val feedbackKo: String,
    val suggestedSummaryKo: String,
    val coreStructure: CoreStructure,
    val keywordsCore: List<String>,
    val wordsOptional: List<String>
)
