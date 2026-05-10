package com.molla.domain.auth;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuthCodeRepository extends JpaRepository<AuthCode, String> {

    /**
     * 가장 최근 발급된 미인증 코드 조회.
     * JPQL은 LIMIT 미지원 — Pageable로 처리 후 첫 번째 요소 반환.
     */
    @Query("""
            SELECT a FROM AuthCode a
            WHERE a.phoneNumber = :phoneNumber
              AND a.isVerified = false
            ORDER BY a.createdAt DESC
            """)
    List<AuthCode> findLatestUnverifiedList(
            @Param("phoneNumber") String phoneNumber,
            org.springframework.data.domain.Pageable pageable
    );

    default Optional<AuthCode> findLatestUnverifiedByPhoneNumber(String phoneNumber) {
        return findLatestUnverifiedList(phoneNumber, PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    @Modifying
    @Query("DELETE FROM AuthCode a WHERE a.phoneNumber = :phoneNumber")
    void deleteAllByPhoneNumber(@Param("phoneNumber") String phoneNumber);
}
