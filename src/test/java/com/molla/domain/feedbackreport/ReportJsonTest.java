package com.molla.domain.feedbackreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesStructuredReport() throws Exception {
        String json = """
                {
                  "oneLineSummary": "따듯하고 자연스럽게 대화를 이끌었지만 문장 정확도는 조금 더 다듬을 수 있어요.",
                  "levelPercentage": 27,
                  "levelAnalysis": "표현 의도는 잘 전달되지만 문장 구조 안정성이 조금 더 필요합니다.",
                  "coreSentences": [
                    {
                      "sourceTurnIndex": 3,
                      "originSentence": "She go to school",
                      "improvedSentence": "She goes to school every morning.",
                      "keyExpression": "goes to school",
                      "keyExpressionKorean": "학교에 다니다",
                      "sampleRate": 16000,
                      "audioKey": "calls/test/turn-3.wav",
                      "audioUrl": "https://signed-url"
                    }
                  ],
                  "habitAnalyses": [
                    {
                      "habit": "짧은 문장 반복",
                      "evidence": "I like it. I use it. I want it.",
                      "suggestion": "문장을 연결해 조금 더 길게 말해보세요."
                    }
                  ],
                  "scores": [
                    {"exam": "IELTS", "score": "6.0"},
                    {"exam": "TOEIC", "score": "780"},
                    {"exam": "OPIC", "score": "IM2"}
                  ],
                  "weakPoints": ["시제 일관성", "3인칭 단수 동사 활용"],
                  "levelResult": "상위 30%"
                }
                """;

        Report report = objectMapper.readValue(json, Report.class);

        assertThat(report.oneLineSummary()).contains("문장 정확도");
        assertThat(report.levelPercentage()).isEqualTo(27);
        assertThat(report.levelAnalysis()).contains("문장 구조 안정성");
        assertThat(report.coreSentences()).hasSize(1);
        assertThat(report.coreSentences().get(0).sourceTurnIndex()).isEqualTo(3);
        assertThat(report.coreSentences().get(0).originSentence()).isEqualTo("She go to school");
        assertThat(report.coreSentences().get(0).keyExpression()).isEqualTo("goes to school");
        assertThat(report.coreSentences().get(0).keyExpressionKorean()).isEqualTo("학교에 다니다");
        assertThat(report.coreSentences().get(0).sampleRate()).isEqualTo(16000);
        assertThat(report.coreSentences().get(0).audioKey()).isEqualTo("calls/test/turn-3.wav");
        assertThat(report.coreSentences().get(0).audioUrl()).isEqualTo("https://signed-url");
        assertThat(report.habitAnalyses()).hasSize(1);
        assertThat(report.scores()).hasSize(3);
        assertThat(report.scores().get(2).exam()).isEqualTo("OPIC");
        assertThat(report.weakPoints()).contains("시제 일관성");
        assertThat(report.levelResult()).isEqualTo("상위 30%");
    }
}
