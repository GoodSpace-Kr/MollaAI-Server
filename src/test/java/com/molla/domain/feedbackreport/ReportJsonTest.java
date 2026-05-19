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
                  "coreSentences": [
                    {
                      "sentence": "She go to school",
                      "grammarCorrection": "She goes to school",
                      "improvedSentence": "She goes to school every morning."
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
        assertThat(report.coreSentences()).hasSize(1);
        assertThat(report.coreSentences().get(0).grammarCorrection()).isEqualTo("She goes to school");
        assertThat(report.habitAnalyses()).hasSize(1);
        assertThat(report.scores()).hasSize(3);
        assertThat(report.scores().get(2).exam()).isEqualTo("OPIC");
        assertThat(report.weakPoints()).contains("시제 일관성");
        assertThat(report.levelResult()).isEqualTo("상위 30%");
    }
}
