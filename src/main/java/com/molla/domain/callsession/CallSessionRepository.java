package com.molla.domain.callsession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CallSessionRepository extends JpaRepository<CallSession, String> {

    /** 유저의 통화 목록 — 최신순 */
    List<CallSession> findByUserIdOrderByStartedAtDesc(String userId);

    /** 유저의 특정 세션 조회 (본인 것인지 확인용) */
    Optional<CallSession> findByIdAndUserId(String id, String userId);

    /**
     * 오늘 완료된 통화의 총 duration_seconds 합산.
     * DailyUsageCalculator 구현에 사용.
     */
    @Query("""
            SELECT COALESCE(SUM(c.durationSeconds), 0)
            FROM CallSession c
            WHERE c.userId = :userId
              AND c.status = 'completed'
              AND c.startedAt >= :startOfDay
            """)
    int sumDurationSecondsTodayByUserId(
            @Param("userId") String userId,
            @Param("startOfDay") LocalDateTime startOfDay
    );
}
