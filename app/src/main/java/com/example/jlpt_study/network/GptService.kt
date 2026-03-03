package com.example.jlpt_study.network

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

class GptService(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val baseUrl = "https://api.openai.com/v1/chat/completions"

    /**
     * N3 레벨 문장 생성 (실제 시험 독해 수준 + 기능 블록 분리)
     */
    suspend fun generateSentences(count: Int = 10): Result<List<SentenceItem>> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """You are a JLPT N3 reading comprehension test item writer.
You create sentences that match the ACTUAL difficulty of N3 読解 section.
You also split sentences into FUNCTIONAL BLOCKS for reading comprehension training.
Return ONLY valid JSON. No markdown, no extra text."""

            val userPrompt = """Generate $count JLPT N3 reading comprehension level sentences.

=== CRITICAL: ACTUAL N3 EXAM DIFFICULTY ===
These must match real N3 독해 difficulty. NOT simple daily conversation.

REQUIRED N3 GRAMMAR (Use 2-3 per sentence):
- Cause-Effect: ～ため(に)/～ことから/～おかげで/～せいで/～によって
- Contrast: ～のに/～にもかかわらず/～一方(で)/～が/～けれども
- Conjecture: ～らしい/～ようだ/～はずだ/～わけだ
- Hearsay: ～ということだ/～とのことだ/～そうだ
- Formal: ～において/～に関して/～について/～にとって

LENGTH: 50~100 characters per sentence

=== FUNCTIONAL BLOCK SPLITTING (매우 중요!) ===
Split each sentence into meaning units based on grammatical function.
This helps learners understand sentence structure, NOT individual words.

BLOCK SPLITTING RULES:
- TOPIC: ends with は/が (主題/主語)
- OBJECT: ends with を
- LOCATION: ends with に/で/へ/から/まで
- REASON: ends with ために/ので/から (이유절)
- CONTRAST: ends with が/けれども/のに (역접)
- CONDITION: ends with ば/たら/なら (조건)
- CONCLUSION: ends with 予定だ/ことだ/わけだ/らしい/ようだ/はずだ (결론/추측)
- QUOTE: ends with という/ということ (인용)
- OTHER: everything else

EXAMPLE:
Sentence: "このプロジェクトは、予算が足りないために、遅れているが、来月までには完了する予定だということだ。"
Blocks:
- {"text": "このプロジェクトは、", "function": "TOPIC"}
- {"text": "予算が足りないために、", "function": "REASON"}
- {"text": "遅れているが、", "function": "CONTRAST"}
- {"text": "来月までには完了する予定だということだ。", "function": "CONCLUSION"}

For each item return:
- jp: Full Japanese sentence
- blocks: Array of {"text": "...", "function": "TOPIC|REASON|CONTRAST|CONDITION|CONCLUSION|QUOTE|LOCATION|OBJECT|OTHER"}
- gold_summary_ko: Korean summary (핵심만)
- keywords_core: 3 key words
- tags: grammar patterns

JSON format:
{ "items": [ { "jp": "...", "blocks": [{"text": "...", "function": "..."}], "gold_summary_ko": "...", "keywords_core": [...], "tags": [...] } ] }"""

            val response = callGpt(systemPrompt, userPrompt)
            val parsed = gson.fromJson(response, GeneratedSentencesResponse::class.java)
            
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
            val systemPrompt = """You are an evaluator for a JLPT N3 reading-speed training app.
Judge whether the user's Korean one-line summary matches the meaning of the Japanese sentence.
Allow paraphrases. Focus on core meaning (who/what/cause/result).
Return ONLY valid JSON."""

            val userPrompt = """jp_sentence: "$jpSentence"
gold_summary_ko: "$goldSummaryKo"
user_summary_ko: "$userSummaryKo"
unknown_words: ${gson.toJson(unknownWords)}

Return JSON:
{
  "is_correct": boolean,
  "match_score": number,
  "error_type": "particle"|"verb"|"vocab"|"logic"|"missing_info"|"none",
  "one_line_feedback_ko": string,
  "suggested_summary_ko": string,
  "core_structure": { "time": string|null, "cause": string|null, "result": string|null },
  "keywords_core": string[],
  "words_optional": string[]
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
    val time: String?,
    val cause: String?,
    val result: String?
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
