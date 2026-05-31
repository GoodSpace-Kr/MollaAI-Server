package com.molla.controller.dto.feedbackreport;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "통화 스크립트 턴 응답")
public record TranscriptTurnResponse(

        @Schema(description = "턴 순번", example = "1")
        Integer index,

        @Schema(description = "턴 생성 시각", example = "2026-05-20T12:00:01.123456+00:00")
        OffsetDateTime createdAt,

        @Schema(description = "사용자 발화")
        UserTurnResponse user,

        @Schema(description = "어시스턴트 발화")
        AssistantTurnResponse assistant
) {
    @Schema(description = "사용자 발화 응답")
    public record UserTurnResponse(

            @Schema(description = "음성 인식 텍스트", example = "Hello, I want to practice English.")
            String text,

            @Schema(description = "오디오 샘플레이트", example = "16000")
            Integer sampleRate,

            @Schema(description = "S3에 저장된 오디오 파일 키", example = "calls/CA.../turns/1.wav")
            String audioKey,

            @Schema(description = "원문 오디오 재생용 presigned URL", example = "https://signed-url")
            String audioUrl
    ) {
    }

    @Schema(description = "어시스턴트 발화 응답")
    public record AssistantTurnResponse(

            @Schema(description = "어시스턴트 응답 텍스트", example = "Sure, let's get started.")
            String text,

            @Schema(description = "응답 생성 시각", example = "2026-05-20T12:00:02.234567+00:00")
            OffsetDateTime createdAt
    ) {
    }
}
