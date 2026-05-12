package com.molla.domain.feedbackreport;

import com.molla.common.response.ErrorCode;
import com.molla.controller.dto.feedbackreport.FeedbackReportResponse;
import com.molla.controller.dto.feedbackreport.FeedbackReportSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackReportService {

    private final FeedbackReportRepository feedbackReportRepository;

    // ──────────────────────────────────────────────
    // 리포트 목록 조회 (프론트용)
    // ──────────────────────────────────────────────

    public List<FeedbackReportSummaryResponse> getMyReports(String userId) {
        return feedbackReportRepository.findAllByUserId(userId)
                .stream()
                .map(FeedbackReportSummaryResponse::from)
                .toList();
    }

    // ──────────────────────────────────────────────
    // 리포트 상세 조회 (프론트용)
    // ──────────────────────────────────────────────

    public FeedbackReportResponse getReport(String sessionId, String userId) {
        FeedbackReport report = feedbackReportRepository
                .findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new FeedbackReportException(ErrorCode.REPORT_NOT_FOUND));

        return FeedbackReportResponse.from(report);
    }
}
