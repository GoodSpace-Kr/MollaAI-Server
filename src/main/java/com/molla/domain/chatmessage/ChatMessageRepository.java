package com.molla.domain.chatmessage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    /** 유저의 전체 채팅 목록 — 시간순 */
    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(String userId);
}
