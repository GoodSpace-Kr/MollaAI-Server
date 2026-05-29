package com.molla.domain.feedbackreport;

import com.molla.common.response.ErrorCode;
import com.molla.controller.dto.feedbackreport.FeedbackReportResponse;
import com.molla.controller.dto.feedbackreport.FeedbackReportSummaryResponse;
import com.molla.domain.callsession.CallSession;
import com.molla.domain.callsession.CallSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackReportService {

    private final FeedbackReportRepository feedbackReportRepository;
    private final CallSessionRepository callSessionRepository;
    private final FeedbackReportViewMapper feedbackReportViewMapper;

    // ──────────────────────────────────────────────
    // 리포트 목록 조회 (프론트용)
    // ──────────────────────────────────────────────

    public List<FeedbackReportSummaryResponse> getMyReports(String userId) {
        List<FeedbackReport> reports = feedbackReportRepository.findAllByUserId(userId);
        Map<String, CallSession> sessionsById = callSessionRepository.findAllById(
                        reports.stream()
                                .map(FeedbackReport::getSessionId)
                                .toList()
                ).stream()
                .collect(Collectors.toMap(CallSession::getId, Function.identity()));

        return reports
                .stream()
                .map(report -> feedbackReportViewMapper.toSummaryResponse(report, sessionsById.get(report.getSessionId())))
                .toList();
    }

    // ──────────────────────────────────────────────
    // 리포트 상세 조회 (프론트용)
    // ──────────────────────────────────────────────

    public FeedbackReportResponse getReport(String sessionId, String userId) {
        FeedbackReport report = feedbackReportRepository
                .findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new FeedbackReportException(ErrorCode.REPORT_NOT_FOUND));

        CallSession session = callSessionRepository.findById(report.getSessionId())
                .orElseThrow(() -> new FeedbackReportException(ErrorCode.SESSION_NOT_FOUND));

        return feedbackReportViewMapper.toDetailResponse(report, session);
    }
}
