package com.molla.domain.conversationturn;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationTurnRepository extends JpaRepository<ConversationTurn, String> {

    /** 세션의 전체 발화 — sequence_order 오름차순 */
    List<ConversationTurn> findBySessionIdOrderBySequenceOrderAsc(String sessionId);

    /** 세션의 마지막 sequence_order 조회 (다음 순번 계산용) */
    int countBySessionId(String sessionId);
}
