package com.molla.controller.dto.callsession;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "통화 세션 종료 요청 (내부 API — AI 오케스트레이션 서버 전용)")
public record EndSessionRequest(

        @Schema(description = "종료 상태 (completed / failed). 기본값: completed", example = "completed")
        String status,

        @Schema(description = "통화 내용 전문", example = "Hello. I am Joshua. i want to ...")
        String transcript,

        @Schema(description = "통화 발화 목록")
        List<UtterancePayload> utterances
) {
    public String resolvedStatus() {
        return (status != null && "failed".equals(status)) ? "failed" : "completed";
    }

    @Schema(description = "통화 발화 데이터")
    public record UtterancePayload(
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
}
