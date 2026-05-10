package com.molla.domain.feedbackreport;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackReportRepository extends JpaRepository<FeedbackReport, String> {

    Optional<FeedbackReport> findBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);

    /**
     * 유저의 리포트 목록 — 서브쿼리로 CallSession 조인
     * @ManyToOne 연관관계 없이 String ID로만 관리하므로 cross join 방지
     */
    @Query("""
            SELECT r FROM FeedbackReport r
            WHERE r.sessionId IN (
                SELECT c.id FROM CallSession c
                WHERE c.userId = :userId
                  AND c.status = 'completed'
            )
            ORDER BY r.createdAt DESC
            """)
    List<FeedbackReport> findAllByUserId(@Param("userId") String userId);

    /**
     * 특정 세션의 리포트 — 본인 세션인지 동시에 확인
     */
    @Query("""
            SELECT r FROM FeedbackReport r
            WHERE r.sessionId = :sessionId
              AND r.sessionId IN (
                  SELECT c.id FROM CallSession c
                  WHERE c.userId = :userId
              )
            """)
    Optional<FeedbackReport> findBySessionIdAndUserId(
            @Param("sessionId") String sessionId,
            @Param("userId") String userId
    );
}
