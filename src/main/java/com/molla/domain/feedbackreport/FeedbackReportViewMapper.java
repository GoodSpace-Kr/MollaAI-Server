package com.molla.domain.feedbackreport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.common.response.ErrorCode;
import com.molla.domain.callsession.CallSession;
import com.molla.domain.callsession.CallSessionTurn;
import com.molla.controller.dto.feedbackreport.FeedbackReportResponse;
import com.molla.controller.dto.feedbackreport.FeedbackReportSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FeedbackReportViewMapper {

    private final ObjectMapper objectMapper;
    private final com.molla.domain.worker.S3AudioUrlService s3AudioUrlService;

    public FeedbackReportSummaryResponse toSummaryResponse(FeedbackReport report) {
        return toSummaryResponse(report, null);
    }

    public FeedbackReportSummaryResponse toSummaryResponse(FeedbackReport report, CallSession session) {
        return new FeedbackReportSummaryResponse(
                report.getId(),
                report.getSessionId(),
                report.getReportType(),
                report.getOneLineSummary(),
                readList(report.getScores(), new TypeReference<List<Report.ReportScore>>() {
                }, "scores"),
                report.getLevelPercentage(),
                report.getLevelAnalysis(),
                report.getLevelResult(),
                toDurationMinutes(session),
                report.getCreatedAt()
        );
    }

    public FeedbackReportResponse toDetailResponse(FeedbackReport report, CallSession session) {
        List<Report.CoreSentenceFeedback> coreSentences = readList(
                report.getCoreSentences(),
                new TypeReference<List<Report.CoreSentenceFeedback>>() {
                },
                "coreSentences"
        ).stream()
                .map(this::attachAudioUrl)
                .toList();
        List<CallSessionTurn> transcript = session != null
                ? readList(session.getTurnsJson(), new TypeReference<List<CallSessionTurn>>() {
                }, "turnsJson")
                : List.of();

        return new FeedbackReportResponse(
                report.getId(),
                report.getSessionId(),
                report.getReportType(),
                report.getOneLineSummary(),
                coreSentences,
                readList(report.getHabitAnalyses(), new TypeReference<List<Report.HabitAnalysis>>() {
                }, "habitAnalyses"),
                readList(report.getScores(), new TypeReference<List<Report.ReportScore>>() {
                }, "scores"),
                readList(report.getWeakPoints(), new TypeReference<List<String>>() {
                }, "weakPoints"),
                transcript,
                report.getLevelPercentage(),
                report.getLevelAnalysis(),
                report.getLevelResult(),
                session != null ? session.getStartedAt() : null,
                toDurationMinutes(session),
                report.getCreatedAt()
        );
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> typeReference, String fieldName) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new FeedbackReportException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "리포트 응답 변환 실패: " + fieldName
            );
        }
    }

    private Report.CoreSentenceFeedback attachAudioUrl(Report.CoreSentenceFeedback coreSentence) {
        if (coreSentence == null) {
            return null;
        }

        return new Report.CoreSentenceFeedback(
                coreSentence.sourceTurnIndex(),
                coreSentence.originSentence(),
                coreSentence.improvedSentence(),
                coreSentence.keyExpression(),
                coreSentence.keyExpressionKorean(),
                coreSentence.sampleRate(),
                coreSentence.audioKey(),
                s3AudioUrlService.createAudioUrl(coreSentence.audioKey())
        );
    }

    private Integer toDurationMinutes(CallSession session) {
        if (session == null || session.getDurationSeconds() == null) {
            return null;
        }
        return session.getDurationSeconds() / 60;
    }
}
