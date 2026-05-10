package com.molla.controller.dto.feedbackreport;

import com.molla.domain.feedbackreport.FeedbackReport;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "리포트 목록 응답 (요약)")
public record FeedbackReportSummaryResponse(

        @Schema(description = "리포트 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "리포트 타입 (level_test / practice)", example = "practice")
        String reportType,

        @Schema(description = "한 줄 요약", example = "전반적으로 유창하나 3인칭 단수 동사 누락이 반복됩니다.")
        String oneLineSummary,

        @Schema(description = "종합 점수 (0~100)", example = "78.5")
        Float overallScore,

        @Schema(description = "레벨 결과 (level_test 타입만 사용)", example = "상위 23%")
        String levelResult,

        @Schema(description = "리포트 생성 일시")
        LocalDateTime createdAt
) {
    public static FeedbackReportSummaryResponse from(FeedbackReport report) {
        return new FeedbackReportSummaryResponse(
                report.getId(),
                report.getSessionId(),
                report.getReportType(),
                report.getOneLineSummary(),
                report.getOverallScore(),
                report.getLevelResult(),
                report.getCreatedAt()
        );
    }
}
