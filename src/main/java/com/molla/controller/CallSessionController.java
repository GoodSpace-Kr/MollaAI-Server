package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.controller.dto.callsession.CallSessionResponse;
import com.molla.controller.dto.callsession.EndSessionRequest;
import com.molla.controller.dto.callsession.StartSessionRequest;
import com.molla.domain.callsession.CallSessionService;
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

@Tag(name = "CallSession", description = "통화 세션 API")
@RestController
@RequiredArgsConstructor
public class CallSessionController {

    private final CallSessionService callSessionService;

    // ──────────────────────────────────────────────
    // 내부 API (AI 오케스트레이션 서버 전용)
    // ──────────────────────────────────────────────

    @Operation(
            summary = "[내부] 통화 세션 시작",
            description = """
                    AI 오케스트레이션 서버가 통화 연결 시 호출합니다.
                    - 전화번호로 유저를 조회하고, 없으면 미가입 유저와 데모 premium 구독을 생성합니다.
                    - 해당 전화번호의 첫 통화면 level_test, 아니면 practice로 자동 결정합니다.
                    - 유저 상태(user_state_at_call) 스냅샷을 저장합니다.
                    - 응답에는 현재 활성 구독 정보와 오늘 잔여 통화 시간도 포함됩니다.
                    - 이 API는 JWT 인증 없이 호출됩니다 (내부망 전용).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "세션 시작 성공",
                    content = @Content(schema = @Schema(implementation = CallSessionResponse.class)))
    })
    @PostMapping("/api/v1/internal/sessions/start")
    public ResponseEntity<ApiResponse<CallSessionResponse>> startSession(
            @RequestBody @Valid StartSessionRequest request
    ) {
        CallSessionResponse response = callSessionService.startSession(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "[내부] 통화 세션 종료",
            description = """
                    AI 오케스트레이션 서버가 통화 종료 시 호출합니다.
                    - 요청 본문의 durationMinutes를 실제 통화 시간(분)으로 받아 초 단위로 변환해 저장합니다.
                    - 저장된 통화 시간은 이후 구독의 오늘 잔여 통화 시간 계산에도 반영됩니다.
                    - completed 상태면 비동기 워커(리포트 생성, 메모리 업로드)를 트리거합니다.
                    - 3분 미만 completed 세션은 워커 후처리를 건너뜁니다.
                    - 이 API는 JWT 인증 없이 호출됩니다 (내부망 전용).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "세션 종료 성공",
                    content = @Content(schema = @Schema(implementation = CallSessionResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "세션 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "이미 종료된 세션",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PatchMapping("/api/v1/internal/sessions/{id}/end")
    public ResponseEntity<ApiResponse<CallSessionResponse>> endSession(
            @PathVariable String id,
            @RequestBody(required = false) EndSessionRequest request
    ) {
        CallSessionResponse response = callSessionService.endSession(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ──────────────────────────────────────────────
    // 프론트 API
    // ──────────────────────────────────────────────

    @Operation(
            summary = "내 통화 목록 조회",
            description = "JWT로 인증된 유저의 전체 통화 기록을 최신순으로 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CallSessionResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/api/v1/sessions")
    public ResponseEntity<ApiResponse<List<CallSessionResponse>>> getMySessions() {
        String userId = getCurrentUserId();
        List<CallSessionResponse> response = callSessionService.getMySessions(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "통화 상세 조회",
            description = "특정 통화 세션의 상세 정보를 반환합니다. 본인 세션만 조회 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CallSessionResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "세션 없음 또는 권한 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/api/v1/sessions/{id}")
    public ResponseEntity<ApiResponse<CallSessionResponse>> getSession(
            @PathVariable String id
    ) {
        String userId = getCurrentUserId();
        CallSessionResponse response = callSessionService.getSession(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
