package com.molla.controller.dto.callsession;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "통화 세션 시작 요청 (내부 API — AI 오케스트레이션 서버 전용)")
public record StartSessionRequest(

        @Schema(description = "유저 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotBlank(message = "userId를 입력해주세요.")
        String userId,

        @Schema(description = "통화 공급자가 발급한 통화 ID", example = "CA1234abcd")
        String callSid,

        @Schema(description = "AI 서버 WebSocket 세션 ID", example = "ws-session-abc123")
        String aiWsSessionId,

        @Schema(description = "세션 타입 (level_test / practice)", example = "practice")
        @NotBlank(message = "sessionType을 입력해주세요.")
        @Pattern(regexp = "^(level_test|practice)$", message = "sessionType은 level_test 또는 practice여야 합니다.")
        String sessionType,

        @Schema(description = "통화 주제", example = "비즈니스 미팅 영어")
        String topic
) {}
