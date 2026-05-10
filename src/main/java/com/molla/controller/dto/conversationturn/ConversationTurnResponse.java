package com.molla.controller.dto.conversationturn;

import com.molla.domain.conversationturn.ConversationTurn;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "발화 기록 응답")
public record ConversationTurnResponse(

        @Schema(description = "발화 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,

        @Schema(description = "발화자 (user / ai)", example = "user")
        String speaker,

        @Schema(description = "발화 내용", example = "I would like to schedule a meeting for next Monday.")
        String content,

        @Schema(description = "STT 신뢰도 (0.0 ~ 1.0). AI 발화는 null", example = "0.95")
        Float confidenceScore,

        @Schema(description = "발화 구간 녹음 파일 경로", example = "https://storage.molla.ai/audio/abc123.wav")
        String audioUrl,

        @Schema(description = "발화 순서", example = "1")
        int sequenceOrder,

        @Schema(description = "발화 시각")
        LocalDateTime createdAt
) {
    public static ConversationTurnResponse from(ConversationTurn turn) {
        return new ConversationTurnResponse(
                turn.getId(),
                turn.getSessionId(),
                turn.getSpeaker(),
                turn.getContent(),
                turn.getConfidenceScore(),
                turn.getAudioUrl(),
                turn.getSequenceOrder(),
                turn.getCreatedAt()
        );
    }
}
