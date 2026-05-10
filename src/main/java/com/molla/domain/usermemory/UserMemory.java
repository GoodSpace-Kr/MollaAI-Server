package com.molla.domain.usermemory;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_memories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMemory {

    @Id
    @Column(length = 36, columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "user_id", nullable = false, length = 36, unique = true, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String summary;

    // JSON 컬럼 — String으로 저장, 애플리케이션에서 파싱
    @Column(name = "weak_points", columnDefinition = "JSON")
    private String weakPoints;

    @Column(name = "habit_patterns", columnDefinition = "JSON")
    private String habitPatterns;

    @Column(name = "interests", columnDefinition = "JSON")
    private String interests;

    @Column(columnDefinition = "TEXT")
    private String goals;

    @Column(name = "total_call_minutes", nullable = false)
    private int totalCallMinutes;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ──────────────────────────────────────────────
    // 정적 팩토리
    // ──────────────────────────────────────────────

    public static UserMemory create(String userId) {
        UserMemory memory = new UserMemory();
        memory.id = UUID.randomUUID().toString();
        memory.userId = userId;
        memory.totalCallMinutes = 0;
        memory.updatedAt = LocalDateTime.now();
        return memory;
    }

    // ──────────────────────────────────────────────
    // 비즈니스 메서드
    // ──────────────────────────────────────────────

    public void update(
            String summary,
            String weakPoints,
            String habitPatterns,
            String interests,
            String goals,
            int addedMinutes
    ) {
        if (summary != null) this.summary = summary;
        if (weakPoints != null) this.weakPoints = weakPoints;
        if (habitPatterns != null) this.habitPatterns = habitPatterns;
        if (interests != null) this.interests = interests;
        if (goals != null) this.goals = goals;
        this.totalCallMinutes += addedMinutes;
        this.updatedAt = LocalDateTime.now();
    }
}
