package com.molla.controller.dto.chatmessage;

import com.molla.domain.chatmessage.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅 메시지 응답")
public record ChatMessageResponse(

        @Schema(description = "메시지 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "발신자 (user / ai)", example = "user")
        String sender,

        @Schema(description = "메시지 내용", example = "오늘 통화에서 제가 자주 틀린 문법이 뭔가요?")
        String content,

        @Schema(description = "메시지 전송 시각")
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getSender(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
