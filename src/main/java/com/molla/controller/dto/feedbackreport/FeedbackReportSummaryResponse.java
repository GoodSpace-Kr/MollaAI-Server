package com.molla.controller.dto.feedbackreport;

import com.molla.domain.feedbackreport.Report;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

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

        @Schema(
                description = "시험 점수 목록",
                example = "[{\"exam\":\"IELTS\",\"score\":\"6.0\"},{\"exam\":\"TOEIC\",\"score\":\"780\"},{\"exam\":\"OPIC\",\"score\":\"IM2\"}]"
        )
        List<Report.ReportScore> scores,

        @Schema(description = "레벨 퍼센트", example = "27")
        Integer levelPercentage,

        @Schema(description = "현재 영어 수준 분석", example = "표현 의도는 잘 전달되지만 문장 구조 안정성이 조금 더 필요합니다.")
        String levelAnalysis,

        @Schema(description = "레벨 결과 (level_test 타입만 사용)", example = "상위 23%")
        String levelResult,

        @Schema(description = "해당 통화 세션 통화 시간(분)", example = "3")
        Integer sessionDurationMinutes,

        @Schema(description = "리포트 생성 일시")
        LocalDateTime createdAt
) {
}
