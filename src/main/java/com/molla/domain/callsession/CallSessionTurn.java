package com.molla.domain.callsession;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record CallSessionTurn(
        Integer index,
        OffsetDateTime createdAt,
        UserTurn user,
        AssistantTurn assistant
) {
    public record UserTurn(
            String text,
            Integer sampleRate,
            String audioKey
    ) {
    }

    public record AssistantTurn(
            @Schema(description = "AI 응답 텍스트 (영어)", example = "Sure, let's get started.")
            String text,

            @Schema(description = "AI 응답 한국어 번역 — 오케스트레이션에서 null로 보내도 됨, 백엔드 워커가 자동 번역", example = "네, 시작해봐요.")
            String translatedText,

            @Schema(description = "응답 생성 시각", example = "2026-05-20T12:00:02.234567+00:00")
            OffsetDateTime createdAt
    ) {
    }
}
