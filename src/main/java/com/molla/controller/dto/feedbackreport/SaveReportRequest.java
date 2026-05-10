package com.molla.controller.dto.feedbackreport;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "리포트 저장 요청 (내부 워커 전용)")
public record SaveReportRequest(

        @Schema(description = "세션 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotBlank(message = "sessionId를 입력해주세요.")
        String sessionId,

        @Schema(description = "리포트 타입 (level_test / practice)", example = "practice")
        @NotBlank(message = "reportType을 입력해주세요.")
        @Pattern(regexp = "^(level_test|practice)$", message = "reportType은 level_test 또는 practice여야 합니다.")
        String reportType,

        @Schema(description = "한 줄 요약", example = "전반적으로 유창하나 3인칭 단수 동사 누락이 반복됩니다.")
        String oneLineSummary,

        @Schema(
                description = "문법 교정 목록 (JSON 문자열)",
                example = "[{\"original\":\"She go to school\",\"corrected\":\"She goes to school\",\"explanation\":\"3인칭 단수 주어에는 동사에 -s를 붙입니다.\"}]"
        )
        String grammarCorrections,

        @Schema(
                description = "어휘 개선 제안 목록 (JSON 문자열)",
                example = "[{\"used\":\"big\",\"better\":\"significant\",\"reason\":\"비즈니스 맥락에서 더 격식 있는 표현입니다.\"}]"
        )
        String vocabularySuggestions,

        @Schema(
                description = "습관 분석 목록 (JSON 문자열)",
                example = "[{\"pattern\":\"문장 끝에 'right?' 반복\",\"example\":\"It's important, right?\",\"suggestion\":\"다양한 확인 표현을 사용해 보세요.\"}]"
        )
        String habitAnalysis,

        @Schema(description = "발음 노트", example = "th 발음이 d 발음으로 대체되는 경향이 있습니다.")
        String pronunciationNotes,

        @Schema(description = "종합 점수 (0~100)", example = "78.5")
        @DecimalMin(value = "0.0", message = "overallScore는 0.0 이상이어야 합니다.")
        @DecimalMax(value = "100.0", message = "overallScore는 100.0 이하여야 합니다.")
        Float overallScore,

        @Schema(description = "레벨 결과 (level_test 타입만 사용)", example = "상위 23%")
        String levelResult
) {}
