package com.molla.domain.feedbackreport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.molla.controller.dto.feedbackreport.FeedbackReportResponse;
import com.molla.controller.dto.feedbackreport.FeedbackReportSummaryResponse;
import com.molla.controller.dto.feedbackreport.TranscriptTurnResponse;
import com.molla.domain.callsession.CallSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedbackReportViewMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final com.molla.domain.worker.S3AudioUrlService s3AudioUrlService = mock(com.molla.domain.worker.S3AudioUrlService.class);
    private final FeedbackReportViewMapper mapper = new FeedbackReportViewMapper(objectMapper, s3AudioUrlService);

    @Test
    void mapsStructuredDetailResponse() {
        when(s3AudioUrlService.createAudioUrl("calls/test/turn-3.wav")).thenReturn("https://signed-url");
        when(s3AudioUrlService.createAudioUrl("calls/test/turn-1.wav")).thenReturn("https://turn-audio-url");
        CallSession session = mock(CallSession.class);
        when(session.getStartedAt()).thenReturn(LocalDateTime.of(2026, 5, 20, 12, 0));
        when(session.getDurationSeconds()).thenReturn(180);
        when(session.getTurnsJson()).thenReturn("""
                [{"index":1,"createdAt":"2026-05-20T12:00:01.123456+00:00","user":{"text":"Hello, I want to practice English.","sampleRate":16000,"audioKey":"calls/test/turn-1.wav"},"assistant":{"text":"Sure, let's get started.","createdAt":"2026-05-20T12:00:02.234567+00:00"}}]
                """);

        FeedbackReport report = FeedbackReport.create(
                "session-1",
                "practice",
                "따듯하고 안정적인 대화였어요.",
                27,
                "표현 의도는 잘 전달되지만 문장 구조 안정성이 조금 더 필요합니다.",
                """
                [{"sourceTurnIndex":3,"originSentence":"She go to school","improvedSentence":"She usually goes to school early in the morning.","keyExpression":"goes to school","keyExpressionKorean":"학교에 다니다","sampleRate":16000,"audioKey":"calls/test/turn-3.wav"}]
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

        FeedbackReportResponse response = mapper.toDetailResponse(report, session);

        assertThat(response.coreSentences()).hasSize(1);
        assertThat(response.coreSentences().get(0).originSentence()).isEqualTo("She go to school");
        assertThat(response.coreSentences().get(0).keyExpression()).isEqualTo("goes to school");
        assertThat(response.coreSentences().get(0).keyExpressionKorean()).isEqualTo("학교에 다니다");
        assertThat(response.coreSentences().get(0).sourceTurnIndex()).isEqualTo(3);
        assertThat(response.coreSentences().get(0).audioKey()).isEqualTo("calls/test/turn-3.wav");
        assertThat(response.coreSentences().get(0).audioUrl()).isEqualTo("https://signed-url");
        assertThat(response.habitAnalyses()).hasSize(1);
        assertThat(response.scores()).hasSize(3);
        assertThat(response.weakPoints()).containsExactly("시제 일관성", "3인칭 단수 동사 활용");
        assertThat(response.transcript()).isEqualTo(List.of(new TranscriptTurnResponse(
                1,
                OffsetDateTime.parse("2026-05-20T12:00:01.123456+00:00"),
                new TranscriptTurnResponse.UserTurnResponse("Hello, I want to practice English.", 16000, "calls/test/turn-1.wav", "https://turn-audio-url"),
                new TranscriptTurnResponse.AssistantTurnResponse("Sure, let's get started.", OffsetDateTime.parse("2026-05-20T12:00:02.234567+00:00"))
        )));
        assertThat(response.levelPercentage()).isEqualTo(27);
        assertThat(response.levelAnalysis()).contains("문장 구조 안정성");
        assertThat(response.sessionStartedAt()).isEqualTo(LocalDateTime.of(2026, 5, 20, 12, 0));
        assertThat(response.sessionDurationMinutes()).isEqualTo(3);
    }

    @Test
    void mapsStructuredSummaryResponse() {
        FeedbackReport report = FeedbackReport.create(
                "session-2",
                "level_test",
                "전반적으로 자신감이 좋아요.",
                41,
                "상위권 진입 직전 단계로, 답변 확장과 정확도 보완이 필요합니다.",
                "[]",
                "[]",
                """
                [{"exam":"IELTS","score":"6.5"},{"exam":"TOEIC","score":"820"},{"exam":"OPIC","score":"IH"}]
                """,
                "[]",
                "상위 20%"
        );
        CallSession session = mock(CallSession.class);
        when(session.getDurationSeconds()).thenReturn(240);

        FeedbackReportSummaryResponse response = mapper.toSummaryResponse(report, session);

        assertThat(response.scores()).hasSize(3);
        assertThat(response.scores().get(2).exam()).isEqualTo("OPIC");
        assertThat(response.levelResult()).isEqualTo("상위 20%");
        assertThat(response.sessionDurationMinutes()).isEqualTo(4);
        assertThat(response.levelPercentage()).isEqualTo(41);
        assertThat(response.levelAnalysis()).contains("상위권 진입 직전");
    }
}
