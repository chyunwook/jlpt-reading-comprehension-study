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
     * N3 레벨 문장 생성 (독해 수준)
     */
    suspend fun generateSentences(count: Int = 10): Result<List<SentenceItem>> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """You are a JLPT N3 reading comprehension question generator.
Generate sentences at ACTUAL N3 reading test difficulty level.
Return ONLY valid JSON. No extra text or markdown."""

            val userPrompt = """Generate $count JLPT N3-level Japanese sentences for reading comprehension training.

DIFFICULTY REQUIREMENTS (Important - must be N3 reading level, not easy):
- Length: 40~80 characters (복문 or 중문 preferred)
- Must include AT LEAST 2 of these N3 grammar patterns per sentence:
  * Conditional: ば/たら/なら/と
  * Cause/reason: ため(に)/によって/おかげで/せいで
  * Contrast: のに/くせに/にもかかわらず/一方で
  * Conjecture: らしい/ようだ/みたいだ/はずだ/わけだ
  * Passive/Causative: れる・られる/せる・させる
  * Nominalization: こと/の/ということ
  * Complex endings: ことになる/ことにする/ようになる/ようにする
  * Formal expressions: において/に関して/によると/にとって

CONTENT REQUIREMENTS:
- Topics: 사회 문제, 직장 생활, 규칙/안내문, 뉴스 기사 스타일, 의견/주장
- Include some sentences with embedded clauses (関係節)
- Mix of です/ます and plain form
- Some keigo expressions (お/ご～になる, いただく)

For each sentence, provide:
- jp: The Japanese sentence
- gold_summary_ko: Natural Korean summary capturing the KEY POINT (not literal translation)
- keywords_core: 3 most important words that determine meaning
- tags: grammar patterns used, topic

Return JSON:
{ "items": [ { "jp": "...", "gold_summary_ko": "...", "keywords_core": ["...", "...", "..."], "tags": ["...", "..."] }, ... ] }"""

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
