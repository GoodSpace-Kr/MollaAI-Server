package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.controller.dto.feedbackreport.FeedbackReportResponse;
import com.molla.controller.dto.feedbackreport.FeedbackReportSummaryResponse;
import com.molla.controller.dto.feedbackreport.SaveReportRequest;
import com.molla.domain.feedbackreport.FeedbackReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "FeedbackReport", description = "피드백 리포트 API")
@RestController
@RequiredArgsConstructor
public class FeedbackReportController {

    private final FeedbackReportService feedbackReportService;

    @Operation(
            summary = "[내부] 리포트 저장",
            description = """
                    통화 종료 후 비동기 워커가 OpenAI로 생성한 리포트를 저장합니다.
                    - 동일 세션에 이미 리포트가 있으면 기존 리포트를 반환합니다 (멱등성 보장).
                    - 이 API는 JWT 인증 없이 호출됩니다 (내부망 전용).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "저장 성공",
                    content = @Content(schema = @Schema(implementation = FeedbackReportResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "요청 데이터 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/api/v1/internal/reports")
    public ResponseEntity<ApiResponse<FeedbackReportResponse>> saveReport(
            @RequestBody @Valid SaveReportRequest request
    ) {
        FeedbackReportResponse response = feedbackReportService.saveReport(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "내 리포트 목록 조회",
            description = """
                    JWT로 인증된 유저의 전체 리포트 목록을 최신순으로 반환합니다.
                    - 목록에는 요약 정보(한 줄 요약, 종합 점수, 레벨 결과)만 포함합니다.
                    - 상세 내용은 `/api/v1/reports/{sessionId}` 로 조회합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = FeedbackReportSummaryResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/api/v1/reports")
    public ResponseEntity<ApiResponse<List<FeedbackReportSummaryResponse>>> getMyReports() {
        String userId = getCurrentUserId();
        List<FeedbackReportSummaryResponse> response = feedbackReportService.getMyReports(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "리포트 상세 조회",
            description = """
                    특정 세션의 리포트 전체 내용을 반환합니다.
                    - sessionId 기준으로 조회합니다.
                    - JSON 컬럼(grammarCorrections, vocabularySuggestions, habitAnalysis)은
                      문자열로 반환되므로 클라이언트에서 파싱해서 사용하세요.
                    - 본인 세션의 리포트만 조회 가능합니다.
                    - 리포트가 아직 생성 중이면 404를 반환합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = FeedbackReportResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "리포트 없음 (생성 중이거나 권한 없음)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/api/v1/reports/{sessionId}")
    public ResponseEntity<ApiResponse<FeedbackReportResponse>> getReport(
            @PathVariable String sessionId
    ) {
        String userId = getCurrentUserId();
        FeedbackReportResponse response = feedbackReportService.getReport(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
