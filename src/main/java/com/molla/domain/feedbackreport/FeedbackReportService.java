package com.molla.domain.feedbackreport;

import com.molla.common.response.ErrorCode;
import com.molla.controller.dto.feedbackreport.FeedbackReportResponse;
import com.molla.controller.dto.feedbackreport.FeedbackReportSummaryResponse;
import com.molla.controller.dto.feedbackreport.SaveReportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackReportService {

    private final FeedbackReportRepository feedbackReportRepository;

    // ──────────────────────────────────────────────
    // 리포트 저장 (비동기 워커 내부 호출)
    // ──────────────────────────────────────────────

    public FeedbackReportResponse saveReport(SaveReportRequest request) {
        // 동일 세션에 리포트 중복 저장 방지
        if (feedbackReportRepository.existsBySessionId(request.sessionId())) {
            log.warn("리포트 중복 저장 시도 — sessionId: {}", request.sessionId());
            return FeedbackReportResponse.from(
                    feedbackReportRepository.findBySessionId(request.sessionId()).get()
            );
        }

        FeedbackReport report = FeedbackReport.create(
                request.sessionId(),
                request.reportType(),
                request.oneLineSummary(),
                request.grammarCorrections(),
                request.vocabularySuggestions(),
                request.habitAnalysis(),
                request.pronunciationNotes(),
                request.overallScore(),
                request.levelResult()
        );

        feedbackReportRepository.save(report);

        log.info("리포트 저장 완료 — sessionId: {}, type: {}", request.sessionId(), request.reportType());
        return FeedbackReportResponse.from(report);
    }

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
