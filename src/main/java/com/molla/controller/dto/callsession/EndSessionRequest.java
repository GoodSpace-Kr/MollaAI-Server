package com.molla.controller.dto.callsession;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.StringJoiner;

@Schema(description = "통화 세션 종료 요청 (내부 API — AI 오케스트레이션 서버 전용)")
public record EndSessionRequest(

        @Schema(description = "종료 상태 (completed / failed). 기본값: completed", example = "completed")
        String status,

        @Schema(description = "통화 턴 목록")
        List<TurnPayload> turns
) {
    public String resolvedStatus() {
        return (status != null && "failed".equals(status)) ? "failed" : "completed";
    }

    public String renderTranscript() {
        if (turns == null || turns.isEmpty()) {
            return null;
        }

        StringJoiner joiner = new StringJoiner("\n");

        for (TurnPayload turn : turns) {
            if (turn == null) {
                continue;
            }

            if (turn.user() != null && hasText(turn.user().text())) {
                joiner.add("USER: " + turn.user().text().trim());
            }

            if (turn.assistant() != null && hasText(turn.assistant().text())) {
                joiner.add("AI: " + turn.assistant().text().trim());
            }
        }

        String transcript = joiner.toString().trim();
        return transcript.isEmpty() ? null : transcript;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Schema(description = "통화 턴 데이터")
    public record TurnPayload(
            @Schema(description = "턴 순번", example = "1")
            Integer index,

            @Schema(description = "턴 생성 시각", example = "2026-05-20T12:00:01.123456+00:00")
            OffsetDateTime createdAt,

            @Schema(description = "사용자 발화")
            UserTurnPayload user,

            @Schema(description = "어시스턴트 발화")
            AssistantTurnPayload assistant
    ) {
    }

    @Schema(description = "사용자 발화 데이터")
    public record UserTurnPayload(
            @Schema(description = "음성 인식 텍스트", example = "Hello, I want to practice English.")
            String text,

            @Schema(description = "오디오 샘플레이트", example = "16000")
            Integer sampleRate,

            @Schema(description = "오디오 인코딩 형식", example = "pcm16le/base64")
            String encoding,

            @Schema(description = "Base64 인코딩된 오디오 데이터")
            String audio
    ) {
    }

    @Schema(description = "어시스턴트 발화 데이터")
    public record AssistantTurnPayload(
            @Schema(description = "어시스턴트 응답 텍스트", example = "Sure, what seems to be the issue?")
            String text,

            @Schema(description = "응답 생성 시각", example = "2026-05-20T12:00:02.234567+00:00")
            OffsetDateTime createdAt
    ) {
    }
}
