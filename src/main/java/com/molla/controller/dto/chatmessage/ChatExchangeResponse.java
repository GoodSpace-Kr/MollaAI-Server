package com.molla.controller.dto.chatmessage;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 전송 응답 — 유저 메시지 + AI 응답 한 쌍")
public record ChatExchangeResponse(

        @Schema(description = "유저가 보낸 메시지")
        ChatMessageResponse userMessage,

        @Schema(description = "AI가 생성한 응답 메시지")
        ChatMessageResponse aiMessage
) {
    public static ChatExchangeResponse of(ChatMessageResponse userMessage, ChatMessageResponse aiMessage) {
        return new ChatExchangeResponse(userMessage, aiMessage);
    }
}
