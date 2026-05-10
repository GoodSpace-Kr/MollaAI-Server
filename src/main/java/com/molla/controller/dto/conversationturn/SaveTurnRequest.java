package com.molla.controller.dto.conversationturn;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "발화 기록 저장 요청 (내부 API — AI 오케스트레이션 서버 전용)")
public record SaveTurnRequest(

        @Schema(description = "세션 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotBlank(message = "sessionId를 입력해주세요.")
        String sessionId,

        @Schema(description = "발화자 (user / ai)", example = "user")
        @NotBlank(message = "speaker를 입력해주세요.")
        @Pattern(regexp = "^(user|ai)$", message = "speaker는 user 또는 ai여야 합니다.")
        String speaker,

        @Schema(description = "발화 내용", example = "I would like to schedule a meeting for next Monday.")
        @NotBlank(message = "content를 입력해주세요.")
        String content,

        @Schema(description = "STT 신뢰도 (0.0 ~ 1.0). AI 발화는 null", example = "0.95")
        @DecimalMin(value = "0.0", message = "confidenceScore는 0.0 이상이어야 합니다.")
        @DecimalMax(value = "1.0", message = "confidenceScore는 1.0 이하여야 합니다.")
        Float confidenceScore,

        @Schema(description = "발화 구간 녹음 파일 경로", example = "https://storage.molla.ai/audio/abc123.wav")
        String audioUrl
) {}
