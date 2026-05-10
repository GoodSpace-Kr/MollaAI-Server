package com.molla.domain.callsession;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "call_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallSession {

    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "user_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "call_sid", length = 100)
    private String callSid;

    @Column(name = "ai_ws_session_id", length = 100)
    private String aiWsSessionId;

    @Column(name = "session_type", nullable = false, length = 20)
    private String sessionType;                  // level_test / practice

    @Column(name = "user_state_at_call", nullable = false, length = 20)
    private String userStateAtCall;              // unregistered / registered / subscribed

    @Column(length = 100)
    private String topic;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(nullable = false, length = 20)
    private String status;                       // in_progress / completed / failed

    // ──────────────────────────────────────────────
    // 정적 팩토리
    // ──────────────────────────────────────────────

    public static CallSession create(
            String userId,
            String callSid,
            String aiWsSessionId,
            String sessionType,
            String userStateAtCall,
            String topic
    ) {
        CallSession session = new CallSession();
        session.id = UUID.randomUUID().toString();
        session.userId = userId;
        session.callSid = callSid;
        session.aiWsSessionId = aiWsSessionId;
        session.sessionType = sessionType;
        session.userStateAtCall = userStateAtCall;
        session.topic = topic;
        session.startedAt = LocalDateTime.now();
        session.status = "in_progress";
        return session;
    }

    // ──────────────────────────────────────────────
    // 비즈니스 메서드
    // ──────────────────────────────────────────────

    public void end() {
        this.endedAt = LocalDateTime.now();
        this.durationSeconds = (int) java.time.Duration.between(this.startedAt, this.endedAt).getSeconds();
        this.status = "completed";
    }

    public void fail() {
        this.endedAt = LocalDateTime.now();
        this.status = "failed";
    }

    public boolean isInProgress() {
        return "in_progress".equals(this.status);
    }

    public boolean isLevelTest() {
        return "level_test".equals(this.sessionType);
    }
}
