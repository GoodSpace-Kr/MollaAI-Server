package com.molla.domain.feedbackreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.controller.dto.feedbackreport.FeedbackReportResponse;
import com.molla.controller.dto.feedbackreport.FeedbackReportSummaryResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackReportViewMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FeedbackReportViewMapper mapper = new FeedbackReportViewMapper(objectMapper);

    @Test
    void mapsStructuredDetailResponse() {
        FeedbackReport report = FeedbackReport.create(
                "session-1",
                "practice",
                "따듯하고 안정적인 대화였어요.",
                """
                [{"sentence":"She go to school","grammarCorrection":"She goes to school","improvedSentence":"She usually goes to school early in the morning."}]
                """,
                """
                [{"habit":"짧은 문장 반복","evidence":"I like it. I use it.","suggestion":"문장을 연결해서 말해보세요."}]
                """,
                """
                [{"exam":"IELTS","score":"6.0"},{"exam":"TOEIC","score":"780"},{"exam":"OPIC","score":"IM2"}]
                """,
                """
                ["시제 일관성","3인칭 단수 동사 활용"]
                """,
                null
        );

        FeedbackReportResponse response = mapper.toDetailResponse(report);

        assertThat(response.coreSentences()).hasSize(1);
        assertThat(response.coreSentences().get(0).grammarCorrection()).isEqualTo("She goes to school");
        assertThat(response.habitAnalyses()).hasSize(1);
        assertThat(response.scores()).hasSize(3);
        assertThat(response.weakPoints()).containsExactly("시제 일관성", "3인칭 단수 동사 활용");
    }

    @Test
    void mapsStructuredSummaryResponse() {
        FeedbackReport report = FeedbackReport.create(
                "session-2",
                "level_test",
                "전반적으로 자신감이 좋아요.",
                "[]",
                "[]",
                """
                [{"exam":"IELTS","score":"6.5"},{"exam":"TOEIC","score":"820"},{"exam":"OPIC","score":"IH"}]
                """,
                "[]",
                "상위 20%"
        );

        FeedbackReportSummaryResponse response = mapper.toSummaryResponse(report);

        assertThat(response.scores()).hasSize(3);
        assertThat(response.scores().get(2).exam()).isEqualTo("OPIC");
        assertThat(response.levelResult()).isEqualTo("상위 20%");
    }
}
