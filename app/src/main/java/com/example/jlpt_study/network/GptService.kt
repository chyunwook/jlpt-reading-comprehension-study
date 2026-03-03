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
            val systemPrompt = """You are a creative JLPT N3 reading comprehension item writer.

IMPORTANT: Generate DIVERSE and VARIED sentences every time.
- Never repeat similar topics or situations
- Use different subjects, verbs, and contexts each time
- Be creative with scenarios

The purpose is speed-reading practice:
- Focus on particles and verbs
- Identify cause, contrast, and conclusion structure"""

            // 다양성을 위해 랜덤 토픽과 문법 선택
            val topics = listOf(
                "회사/직장 (회의, 출장, 프로젝트, 상사, 동료)",
                "학교/교육 (수업, 시험, 선생님, 학생, 동아리)",
                "쇼핑/서비스 (가게, 할인, 환불, 예약, 배송)",
                "건강/병원 (진료, 약, 증상, 운동, 식이)",
                "여행/교통 (전철, 비행기, 호텔, 관광, 지연)",
                "날씨/계절 (비, 태풍, 더위, 추위, 예보)",
                "뉴스/사회 (사건, 조사, 통계, 정책, 변화)",
                "취미/문화 (영화, 음악, 스포츠, 독서, 게임)",
                "음식/요리 (레스토랑, 레시피, 재료, 맛, 건강식)",
                "기술/인터넷 (앱, 업데이트, 오류, 서비스, SNS)",
                "환경/자연 (쓰레기, 재활용, 에너지, 동물, 식물)",
                "가족/인간관계 (부모, 친구, 이웃, 결혼, 육아)"
            ).shuffled().take(5).joinToString(", ")
            
            val grammarFocus = listOf(
                "ため(に)/ので (원인)",
                "が/けれども/のに (역접)",
                "らしい/ようだ/そうだ (추측/전문)",
                "ことになる/ことにする (결정)",
                "はずだ/わけだ (당연/이유)",
                "によると/によれば (출처)",
                "として/にとって (입장)",
                "ばかり/だけ (한정)",
                "ても/でも (양보)",
                "たら/ば/なら (조건)"
            ).shuffled().take(4).joinToString(", ")

            val userPrompt = """Generate $count DIVERSE sentences.

========================
VARIETY IS CRITICAL! (다양성 필수!)
========================
- Each sentence MUST be about a DIFFERENT topic
- Do NOT repeat similar situations
- Mix different grammar patterns
- Vary sentence structures

This session's topics (이번 세션 주제):
$topics

This session's grammar focus (이번 세션 문법):
$grammarFocus

========================
DIFFICULTY REQUIREMENTS
========================
- JLPT N3 読解 level
- Length: 50–100 characters
- Use 2–3 grammar patterns per sentence

Grammar patterns to use:
- Cause: ため(に), ので, ことから, せいで, おかげで, によって
- Contrast: が, けれども, のに, にもかかわらず, 一方(で)
- Hearsay: ということだ, とのことだ, そうだ, らしい
- Conclusion: ようだ, はずだ, わけだ, 予定だ, ことになる
- Condition: たら, ば, なら, ても
- Formal: において, に関して, について, にとって, として

Avoid rare N2/N1 vocabulary.

========================
PHRASE BLOCK SPLIT (조사 기준 분리)
========================
Split sentence into SMALL PHRASE units by particles.
This helps learners click on unknown words easily.

SPLIT RULES:
- Split AFTER each particle: の, が, は, を, に, で, へ, から, まで, より, と, も
- Keep verb/adjective + ending together (変わったが as one block)
- Include punctuation with the previous block (変わったが、)
- 5-10 blocks per sentence

EXAMPLES:
"会社の方針が変わったが、業務には影響がないようだ。"
→ ["会社の", "方針が", "変わったが、", "業務には", "影響が", "ないようだ。"]

"会議は予定通り行うが、参加者は少ないようだ。"
→ ["会議は", "予定通り", "行うが、", "参加者は", "少ないようだ。"]

"彼女は忙しいけれども、手伝うと言っていた。"
→ ["彼女は", "忙しいけれども、", "手伝うと", "言っていた。"]

All blocks use function: "OTHER" (function type not needed anymore)

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
        {"text": "会社の", "function": "OTHER"},
        {"text": "方針が", "function": "OTHER"},
        {"text": "変わったが、", "function": "OTHER"}
      ],
      "gold_summary_ko": "일본어 어순을 따른 한국어 요약 (25자 내외)",
      "keywords_core": [],
      "tags": []
    }
  ]
}

If you cannot follow these instructions exactly, return: {"error":"cannot_comply"}"""

            // 다양성을 위해 높은 temperature 사용 (0.9)
            val response = callGpt(systemPrompt, userPrompt, temperature = 0.9)
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
            val systemPrompt = """You grade JLPT N3 reading answers. Return ONLY JSON."""
            
            val unknownWordsJson = if (unknownWords.isNotEmpty()) {
                "\n모르는단어: ${unknownWords.joinToString(", ")}\n위 단어들의 한글 뜻을 word_meanings에 반환하세요."
            } else ""

            val userPrompt = """일본어: "$jpSentence"
정답: "$goldSummaryKo"
유저: "$userSummaryKo"$unknownWordsJson

=== 정답 기준 ===
1. 의미가 같으면 정답 (동의어 허용)
2. 어순이 같으면 정답
3. 문장이 완성되면 정답

동의어 예시 (모두 정답):
- 증가하다 = 늘다 = 많아지다
- 감소하다 = 줄다 = 적어지다
- 연기되다 = 미뤄지다
- ~한다고 한다 = ~한대 = ~래

=== 오답 기준 ===
- 문장 절반만 씀 → missing_info
- 어순 반대로 → logic  
- 주어 틀림 → particle
- 동사 완전히 다름 → verb

=== JSON ===
{"is_correct":true/false,"match_score":0.0-1.0,"error_type":"none|particle|verb|vocab|logic|missing_info","one_line_feedback_ko":"","suggested_summary_ko":"","core_structure":{"cause":"","result":"","contrast":""},"keywords_core":[],"words_optional":[],"word_meanings":{"일본어":"한글뜻"}}"""

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
                wordsOptional = parsed.wordsOptional,
                wordMeanings = parsed.wordMeanings ?: emptyMap()
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callGpt(systemPrompt: String, userPrompt: String, temperature: Double = 0.7): String {
        val requestBody = GptRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                Message("system", systemPrompt),
                Message("user", userPrompt)
            ),
            temperature = temperature,
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
    @SerializedName("words_optional") val wordsOptional: List<String>,
    @SerializedName("word_meanings") val wordMeanings: Map<String, String>? = null
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
    val wordsOptional: List<String>,
    val wordMeanings: Map<String, String> = emptyMap()  // 일본어 -> 한글뜻
)
