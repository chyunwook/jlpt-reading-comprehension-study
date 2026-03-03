package com.example.jlpt_study.data

import com.example.jlpt_study.data.model.SentenceItem
import java.util.UUID

/**
 * 초기 샘플 문장 데이터
 * GPT 연동 전 테스트용 또는 오프라인 사용을 위한 기본 데이터
 */
object SampleData {
    
    val sampleSentences: List<SentenceItem> = listOf(
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "天気が悪いため、今日のイベントは中止になりました。",
            goldSummaryKo = "날씨가 나빠서 오늘 이벤트가 취소되었다.",
            keywordsCore = listOf("天気", "中止", "ため"),
            tags = listOf("grammar:ため", "structure:cause_result", "topic:weather")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "電車が遅れているので、会議に間に合わないかもしれません。",
            goldSummaryKo = "전철이 늦어서 회의에 늦을 수도 있다.",
            keywordsCore = listOf("電車", "遅れている", "会議"),
            tags = listOf("grammar:ので", "structure:cause_result", "topic:transport")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "このレストランは美味しいですが、少し高いです。",
            goldSummaryKo = "이 레스토랑은 맛있지만 좀 비싸다.",
            keywordsCore = listOf("美味しい", "高い", "ですが"),
            tags = listOf("grammar:ですが", "structure:contrast", "topic:restaurant")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "来月から新しい仕事を始めることになりました。",
            goldSummaryKo = "다음 달부터 새 일을 시작하게 되었다.",
            keywordsCore = listOf("来月", "新しい仕事", "ことになりました"),
            tags = listOf("grammar:ことになる", "structure:result", "topic:work")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "日本語を勉強しているのに、まだ上手に話せません。",
            goldSummaryKo = "일본어를 공부하고 있는데도 아직 잘 못한다.",
            keywordsCore = listOf("勉強している", "のに", "話せません"),
            tags = listOf("grammar:のに", "structure:contrast", "topic:study")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "風邪を引いたから、今日は学校を休みます。",
            goldSummaryKo = "감기에 걸려서 오늘 학교를 쉰다.",
            keywordsCore = listOf("風邪", "から", "休みます"),
            tags = listOf("grammar:から", "structure:cause_result", "topic:health")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "しかし、問題はまだ解決していません。",
            goldSummaryKo = "하지만 문제는 아직 해결되지 않았다.",
            keywordsCore = listOf("しかし", "問題", "解決していません"),
            tags = listOf("grammar:しかし", "structure:contrast", "topic:general")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "このお店は安くて、品質もいいので人気があります。",
            goldSummaryKo = "이 가게는 싸고 품질도 좋아서 인기가 있다.",
            keywordsCore = listOf("安くて", "品質", "人気"),
            tags = listOf("grammar:て形", "structure:cause_result", "topic:shopping")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "明日までにレポートを出さなければなりません。",
            goldSummaryKo = "내일까지 레포트를 내야 한다.",
            keywordsCore = listOf("明日まで", "レポート", "なければなりません"),
            tags = listOf("grammar:なければならない", "structure:obligation", "topic:study")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "彼女は忙しいと言っていましたが、パーティーに来ました。",
            goldSummaryKo = "그녀는 바쁘다고 했지만 파티에 왔다.",
            keywordsCore = listOf("忙しい", "と言っていました", "来ました"),
            tags = listOf("grammar:と言う", "structure:contrast", "topic:social")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "この本を読んでから、考え方が変わりました。",
            goldSummaryKo = "이 책을 읽고 나서 생각이 바뀌었다.",
            keywordsCore = listOf("読んでから", "考え方", "変わりました"),
            tags = listOf("grammar:てから", "structure:sequence", "topic:reading")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "雨が降りそうなので、傘を持って行きます。",
            goldSummaryKo = "비가 올 것 같아서 우산을 가지고 간다.",
            keywordsCore = listOf("降りそう", "ので", "傘"),
            tags = listOf("grammar:そう", "structure:cause_result", "topic:weather")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "駅の近くに新しいカフェがオープンしました。",
            goldSummaryKo = "역 근처에 새 카페가 열렸다.",
            keywordsCore = listOf("駅の近く", "新しい", "オープン"),
            tags = listOf("structure:info", "topic:location")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "時間がないため、タクシーで行くことにしました。",
            goldSummaryKo = "시간이 없어서 택시로 가기로 했다.",
            keywordsCore = listOf("時間がない", "ため", "タクシー"),
            tags = listOf("grammar:ため", "structure:cause_result", "topic:transport")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "彼は日本に住んでいますが、日本語が話せません。",
            goldSummaryKo = "그는 일본에 살지만 일본어를 못한다.",
            keywordsCore = listOf("住んでいます", "ですが", "話せません"),
            tags = listOf("grammar:ですが", "structure:contrast", "topic:language")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "試験の結果が悪かったので、もっと勉強することにしました。",
            goldSummaryKo = "시험 결과가 나빠서 더 공부하기로 했다.",
            keywordsCore = listOf("試験", "結果", "勉強する"),
            tags = listOf("grammar:ので", "structure:cause_result", "topic:study")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "この映画は面白いと聞いていたのに、つまらなかったです。",
            goldSummaryKo = "이 영화가 재미있다고 들었는데 지루했다.",
            keywordsCore = listOf("面白い", "のに", "つまらなかった"),
            tags = listOf("grammar:のに", "structure:contrast", "topic:entertainment")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "健康のために、毎日運動しています。",
            goldSummaryKo = "건강을 위해 매일 운동하고 있다.",
            keywordsCore = listOf("健康", "ために", "運動"),
            tags = listOf("grammar:ために", "structure:purpose", "topic:health")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "ミーティングは午後3時から始まる予定です。",
            goldSummaryKo = "미팅은 오후 3시부터 시작할 예정이다.",
            keywordsCore = listOf("ミーティング", "午後3時", "予定"),
            tags = listOf("grammar:予定", "structure:schedule", "topic:work")
        ),
        SentenceItem(
            id = UUID.randomUUID().toString(),
            jp = "彼女はまだ来ていませんが、もうすぐ着くと思います。",
            goldSummaryKo = "그녀는 아직 안 왔지만 곧 도착할 것 같다.",
            keywordsCore = listOf("まだ", "もうすぐ", "着く"),
            tags = listOf("grammar:と思う", "structure:prediction", "topic:social")
        )
    )
}
