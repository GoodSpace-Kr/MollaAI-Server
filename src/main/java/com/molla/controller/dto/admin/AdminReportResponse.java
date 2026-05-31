package com.molla.controller.dto.admin;

import com.molla.domain.feedbackreport.FeedbackReport;
import java.time.LocalDateTime;

public record AdminReportResponse(
        String id,
        String sessionId,
        String reportType,
        String oneLineSummary,
        Integer levelPercentage,
        String levelResult,
        LocalDateTime createdAt
) {
    public static AdminReportResponse from(FeedbackReport r) {
        return new AdminReportResponse(
                r.getId(), r.getSessionId(), r.getReportType(),
                r.getOneLineSummary(), r.getLevelPercentage(),
                r.getLevelResult(), r.getCreatedAt()
        );
    }
}
