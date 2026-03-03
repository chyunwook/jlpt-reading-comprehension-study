package com.example.jlpt_study.network

import com.example.jlpt_study.data.model.ErrorType
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
     * N3 레벨 문장 생성
     */
    suspend fun generateSentences(count: Int = 10): Result<List<SentenceItem>> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """You generate JLPT N3-level reading training items.
Return ONLY JSON. No extra text."""

            val userPrompt = """Generate $count independent JLPT N3-level Japanese sentences suitable for speed-reading training.
Constraints:
- 25~55 characters each (not too short)
- Include common N3 structures (ため/ので/から/ですが/しかし/ています/ことになりました etc.)
- Everyday contexts (weather, transport, shopping, work, rules)
For each item, return:
- jp
- gold_summary_ko (one-line situation summary in Korean)
- keywords_core (3 Japanese tokens that are essential)
- tags (grammar/structure/topic)

Return JSON:
{ "items": [ ... ] }"""

            val response = callGpt(systemPrompt, userPrompt)
            val parsed = gson.fromJson(response, GeneratedSentencesResponse::class.java)
            
            val sentences = parsed.items.map { item ->
                SentenceItem(
                    id = UUID.randomUUID().toString(),
                    jp = item.jp,
                    goldSummaryKo = item.goldSummaryKo,
                    keywordsCore = item.keywordsCore,
                    tags = item.tags,
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
    @SerializedName("gold_summary_ko") val goldSummaryKo: String,
    @SerializedName("keywords_core") val keywordsCore: List<String>,
    val tags: List<String>
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
