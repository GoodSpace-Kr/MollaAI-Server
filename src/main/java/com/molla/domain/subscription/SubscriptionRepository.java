package com.molla.domain.subscription;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    /**
     * 유저의 현재 활성 구독 조회.
     * JPQL은 LIMIT 미지원 — Pageable로 처리.
     */
    @Query("""
            SELECT s FROM Subscription s
            WHERE s.userId = :userId
              AND s.status = 'active'
              AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
            ORDER BY s.startedAt DESC
            """)
    List<Subscription> findActiveListByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * 활성 구독 수 조회.
     * JPQL에서 COUNT(s) > 0 boolean 반환 불가 — long으로 받아서 > 0 비교.
     */
    @Query("""
            SELECT COUNT(s) FROM Subscription s
            WHERE s.userId = :userId
              AND s.status = 'active'
              AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)
            """)
    long countActiveByUserId(@Param("userId") String userId);

    // ──────────────────────────────────────────────
    // 기본 제공 메서드
    // ──────────────────────────────────────────────

    default Optional<Subscription> findActiveByUserId(String userId) {
        return findActiveListByUserId(userId,
                org.springframework.data.domain.PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    default boolean existsActiveByUserId(String userId) {
        return countActiveByUserId(userId) > 0;
    }
}
