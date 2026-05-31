package com.molla.domain.callsession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CallSessionRepository extends JpaRepository<CallSession, String> {

    boolean existsByPhoneNumber(String phoneNumber);

    /** 전화번호 기준 통화 목록 — 최신순 */
    List<CallSession> findByPhoneNumberOrderByStartedAtDesc(String phoneNumber);

    /** 전화번호 기준 특정 세션 조회 (본인 것인지 확인용) */
    Optional<CallSession> findByIdAndPhoneNumber(String id, String phoneNumber);

    /**
     * 오늘 완료된 통화의 총 duration_seconds 합산.
     * DailyUsageCalculator 구현에 사용.
     */
    @Query("""
            SELECT COALESCE(SUM(c.durationSeconds), 0)
            FROM CallSession c
            WHERE c.phoneNumber = :phoneNumber
              AND c.status = 'completed'
              AND c.startedAt >= :startOfDay
            """)
    int sumDurationSecondsTodayByPhoneNumber(
            @Param("phoneNumber") String phoneNumber,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    // 관리자용 — 전체 세션 목록 최신순
    List<CallSession> findAllByOrderByStartedAtDesc();

    // 유저별 세션 수 집계
    @Query("SELECT COUNT(c) FROM CallSession c WHERE c.userId = :userId AND c.status = 'completed'")
    int countCompletedByUserId(@Param("userId") String userId);

    // 유저별 총 통화 시간
    @Query("SELECT COALESCE(SUM(c.durationSeconds), 0) FROM CallSession c WHERE c.userId = :userId AND c.status = 'completed'")
    int sumDurationSecondsByUserId(@Param("userId") String userId);
}
