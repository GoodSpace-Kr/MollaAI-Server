package com.molla.controller.dto.feedbackreport;

import com.molla.domain.feedbackreport.Report;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "리포트 상세 응답")
public record FeedbackReportResponse(

        @Schema(description = "리포트 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "리포트 타입 (level_test / practice)", example = "practice")
        String reportType,

        @Schema(description = "한 줄 요약", example = "전반적으로 유창하나 3인칭 단수 동사 누락이 반복됩니다.")
        String oneLineSummary,

        @Schema(
                description = "핵심 문장 피드백 목록",
                example = "[{\"sentence\":\"She go to school\",\"grammarCorrection\":\"She goes to school\",\"improvedSentence\":\"She usually goes to school early in the morning.\"}]"
        )
        List<Report.CoreSentenceFeedback> coreSentences,

        @Schema(
                description = "습관 분석 목록",
                example = "[{\"habit\":\"문장 끝에 right? 반복\",\"evidence\":\"It's important, right?\",\"suggestion\":\"확인 표현을 다양하게 바꿔보세요.\"}]"
        )
        List<Report.HabitAnalysis> habitAnalyses,

        @Schema(
                description = "시험 점수 목록",
                example = "[{\"exam\":\"IELTS\",\"score\":\"6.0\"},{\"exam\":\"TOEIC\",\"score\":\"780\"},{\"exam\":\"OPIC\",\"score\":\"IM2\"}]"
        )
        List<Report.ReportScore> scores,

        @Schema(
                description = "약점 목록",
                example = "[\"3인칭 단수 동사 활용\", \"시제 일관성\"]"
        )
        List<String> weakPoints,

        @Schema(description = "레벨 결과 (level_test 타입만 사용)", example = "상위 23%")
        String levelResult,

        @Schema(description = "리포트 생성 일시")
        LocalDateTime createdAt
) {
}
