package com.molla.domain.chatmessage;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "user_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "session_id", length = 36, columnDefinition = "CHAR(36)")
    private String sessionId;           // nullable — 세션과 무관한 채팅도 가능

    @Column(nullable = false, length = 10)
    private String sender;              // user / ai

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ──────────────────────────────────────────────
    // 정적 팩토리
    // ──────────────────────────────────────────────

    public static ChatMessage create(String userId, String sessionId, String sender, String content) {
        ChatMessage message = new ChatMessage();
        message.id = UUID.randomUUID().toString();
        message.userId = userId;
        message.sessionId = sessionId;
        message.sender = sender;
        message.content = content;
        message.createdAt = LocalDateTime.now();
        return message;
    }
}
