package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.controller.dto.conversationturn.ConversationTurnResponse;
import com.molla.controller.dto.conversationturn.SaveTurnRequest;
import com.molla.domain.conversationturn.ConversationTurnService;
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

@Tag(name = "ConversationTurn", description = "발화 기록 API")
@RestController
@RequiredArgsConstructor
public class ConversationTurnController {

    private final ConversationTurnService conversationTurnService;

    @Operation(
            summary = "[내부] 발화 기록 저장",
            description = """
                    AI 오케스트레이션 서버가 실시간으로 발화를 저장할 때 호출합니다.
                    - speaker: `user` (STT 변환 결과) / `ai` (AI 응답 텍스트)
                    - confidenceScore: STT 신뢰도 (AI 발화는 null)
                    - sequenceOrder는 서버에서 자동 부여합니다.
                    - 이 API는 JWT 인증 없이 호출됩니다 (내부망 전용).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "저장 성공",
                    content = @Content(schema = @Schema(implementation = ConversationTurnResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "세션 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "이미 종료된 세션",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/api/v1/internal/turns")
    public ResponseEntity<ApiResponse<ConversationTurnResponse>> saveTurn(
            @RequestBody @Valid SaveTurnRequest request
    ) {
        ConversationTurnResponse response = conversationTurnService.saveTurn(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "세션 발화 목록 조회",
            description = """
                    특정 통화 세션의 전체 발화 기록을 sequence_order 오름차순으로 반환합니다.
                    - 통화 복습 화면에서 대화 내용을 표시할 때 사용합니다.
                    - 본인 세션만 조회 가능합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ConversationTurnResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "세션 없음 또는 권한 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/api/v1/sessions/{sessionId}/turns")
    public ResponseEntity<ApiResponse<List<ConversationTurnResponse>>> getTurnsBySession(
            @PathVariable String sessionId
    ) {
        String userId = getCurrentUserId();
        List<ConversationTurnResponse> response = conversationTurnService.getTurnsBySession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String getCurrentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
