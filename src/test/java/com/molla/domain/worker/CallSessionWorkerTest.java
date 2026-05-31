package com.molla.domain.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.domain.callsession.CallSession;
import com.molla.domain.callsession.CallSessionRepository;
import com.molla.domain.callsession.CallSessionTurn;
import com.molla.domain.callsession.SessionEndedEvent;
import com.molla.domain.feedbackreport.FeedbackReport;
import com.molla.domain.feedbackreport.FeedbackReportRepository;
import com.molla.domain.feedbackreport.Report;
import com.molla.domain.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CallSessionWorkerTest {

    private final CallSessionRepository callSessionRepository = mock(CallSessionRepository.class);
    private final FeedbackReportRepository feedbackReportRepository = mock(FeedbackReportRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final OpenAiClient openAiClient = mock(OpenAiClient.class);
    private final QdrantClient qdrantClient = mock(QdrantClient.class);
    private final ReportAudioEnricher reportAudioEnricher = mock(ReportAudioEnricher.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final CallSessionWorker worker = new CallSessionWorker(
            callSessionRepository,
            feedbackReportRepository,
            userRepository,
            openAiClient,
            qdrantClient,
            reportAudioEnricher,
            objectMapper
    );

    @Test
    void skipsReportAndMemoryUploadWhenCompletedCallIsUnderThreeMinutes() throws Exception {
        CallSession session = mock(CallSession.class);
        when(callSessionRepository.findById("session-1")).thenReturn(Optional.of(session));
        when(session.getDurationSeconds()).thenReturn(179);

        worker.processAfterCall(new SessionEndedEvent("session-1", null, false));

        verifyNoInteractions(openAiClient, reportAudioEnricher, qdrantClient, feedbackReportRepository, userRepository);
    }

    @Test
    void processesReportAndMemoryUploadWhenCompletedCallIsAtLeastThreeMinutes() throws Exception {
        CallSession session = mock(CallSession.class);
        List<CallSessionTurn> turns = List.of(new CallSessionTurn(
                1,
                OffsetDateTime.parse("2026-05-20T07:08:33.742000+00:00"),
                new CallSessionTurn.UserTurn("hello", 16000, "calls/test/turn-1.wav"),
                new CallSessionTurn.AssistantTurn("hi", null, OffsetDateTime.parse("2026-05-20T07:08:34.000000+00:00"))
        ));
        Report report = new Report(
                "summary", 35,
                "문장 정확도를 조금 더 다듬으면 좋습니다.",
                List.of(), List.of(), List.of(), List.of(), null
        );
        FeedbackReport savedReport = mock(FeedbackReport.class);

        when(callSessionRepository.findById("session-2")).thenReturn(Optional.of(session));
        when(session.getDurationSeconds()).thenReturn(180);
        when(session.getTurnsJson()).thenReturn(objectMapper.writeValueAsString(turns));
        when(session.getSessionType()).thenReturn("practice");
        when(session.getPhoneNumber()).thenReturn("01012345678");
        when(openAiClient.translateTexts(any())).thenReturn(List.of("안녕"));
        when(openAiClient.generateReport(any(), any())).thenReturn(report);
        when(reportAudioEnricher.attachTurnAudio(any(), any())).thenReturn(report);
        when(feedbackReportRepository.findBySessionId("session-2")).thenReturn(Optional.empty());
        when(feedbackReportRepository.save(any())).thenReturn(savedReport);

        worker.processAfterCall(new SessionEndedEvent("session-2", null, false));

        verify(openAiClient).generateReport(any(), any());
        verify(qdrantClient).upsertTurns("session-2", null, "01012345678", turns);
        verify(feedbackReportRepository).save(argThat(feedbackReport ->
                feedbackReport.getLevelPercentage() == 35
                        && "문장 정확도를 조금 더 다듬으면 좋습니다.".equals(feedbackReport.getLevelAnalysis())
        ));
        verify(userRepository, never()).findById(any());
    }
}
