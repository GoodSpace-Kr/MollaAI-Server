package com.molla.domain.feedbackreport;

import com.molla.common.response.ErrorCode;
import com.molla.controller.dto.feedbackreport.FeedbackReportResponse;
import com.molla.domain.callsession.CallSession;
import com.molla.domain.callsession.CallSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackReportServiceTest {

    private final FeedbackReportRepository feedbackReportRepository = mock(FeedbackReportRepository.class);
    private final CallSessionRepository callSessionRepository = mock(CallSessionRepository.class);
    private final FeedbackReportViewMapper feedbackReportViewMapper = mock(FeedbackReportViewMapper.class);

    private final FeedbackReportService service = new FeedbackReportService(
            feedbackReportRepository,
            callSessionRepository,
            feedbackReportViewMapper
    );

    @Test
    void getReportLoadsSessionAndReturnsDetailedResponseWithSessionMetadata() {
        FeedbackReport report = FeedbackReport.create(
                "session-1",
                "practice",
                "summary",
                "[]",
                "[]",
                "[]",
                "[]",
                null
        );
        CallSession session = mock(CallSession.class);
        FeedbackReportResponse expected = new FeedbackReportResponse(
                "report-1",
                "session-1",
                "practice",
                "summary",
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                null,
                LocalDateTime.of(2026, 5, 25, 15, 0),
                180,
                LocalDateTime.of(2026, 5, 25, 15, 10)
        );

        when(feedbackReportRepository.findBySessionIdAndUserId("session-1", "user-1")).thenReturn(Optional.of(report));
        when(callSessionRepository.findById("session-1")).thenReturn(Optional.of(session));
        when(feedbackReportViewMapper.toDetailResponse(report, session)).thenReturn(expected);

        FeedbackReportResponse response = service.getReport("session-1", "user-1");

        assertThat(response).isEqualTo(expected);
        verify(callSessionRepository).findById("session-1");
        verify(feedbackReportViewMapper).toDetailResponse(report, session);
    }

    @Test
    void getReportThrowsWhenLinkedSessionDoesNotExist() {
        FeedbackReport report = FeedbackReport.create(
                "session-1",
                "practice",
                "summary",
                "[]",
                "[]",
                "[]",
                "[]",
                null
        );

        when(feedbackReportRepository.findBySessionIdAndUserId("session-1", "user-1")).thenReturn(Optional.of(report));
        when(callSessionRepository.findById("session-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReport("session-1", "user-1"))
                .isInstanceOf(FeedbackReportException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SESSION_NOT_FOUND);
    }
}
