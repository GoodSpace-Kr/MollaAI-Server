package com.molla.domain.conversationturn;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversation_turns")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConversationTurn {

    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "session_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String sessionId;

    @Column(nullable = false, length = 10)
    private String speaker;          // user / ai

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "confidence_score")
    private Float confidenceScore;   // STT 신뢰도 0~1, nullable

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ──────────────────────────────────────────────
    // 정적 팩토리
    // ──────────────────────────────────────────────

    public static ConversationTurn create(
            String sessionId,
            String speaker,
            String content,
            Float confidenceScore,
            String audioUrl,
            int sequenceOrder
    ) {
        ConversationTurn turn = new ConversationTurn();
        turn.id = UUID.randomUUID().toString();
        turn.sessionId = sessionId;
        turn.speaker = speaker;
        turn.content = content;
        turn.confidenceScore = confidenceScore;
        turn.audioUrl = audioUrl;
        turn.sequenceOrder = sequenceOrder;
        turn.createdAt = LocalDateTime.now();
        return turn;
    }
}
