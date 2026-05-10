package com.molla.domain.subscription;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "user_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "plan_type", nullable = false, length = 20)
    private String planType;

    @Column(name = "daily_limit_minutes", nullable = false)
    private int dailyLimitMinutes;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false, length = 20)
    private String status;


    public static Subscription create(
            String userId,
            String planType,
            int dailyLimitMinutes,
            LocalDateTime expiresAt
    ) {
        Subscription sub = new Subscription();
        sub.id = UUID.randomUUID().toString();
        sub.userId = userId;
        sub.planType = planType;
        sub.dailyLimitMinutes = dailyLimitMinutes;
        sub.startedAt = LocalDateTime.now();
        sub.expiresAt = expiresAt;
        sub.status = "active";
        return sub;
    }

    // ──────────────────────────────────────────────
    // 비즈니스 메서드
    // ──────────────────────────────────────────────

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return "active".equals(this.status) && !isExpired();
    }

    public void expire() {
        this.status = "expired";
    }

    public void cancel() {
        this.status = "cancelled";
    }
}
