package com.molla.domain.chatmessage;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;
import com.molla.controller.dto.chatmessage.ChatExchangeResponse;
import com.molla.controller.dto.chatmessage.ChatMessageResponse;
import com.molla.controller.dto.chatmessage.SendMessageRequest;
import com.molla.domain.user.User;
import com.molla.domain.user.UserRepository;
import com.molla.domain.usermemory.UserMemory;
import com.molla.domain.usermemory.UserMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final UserMemoryService userMemoryService;
    private final ChatAiClient chatAiClient;

    // ──────────────────────────────────────────────
    // 전체 채팅 목록 조회
    // ──────────────────────────────────────────────

    public List<ChatMessageResponse> getAllMessages(String userId) {
        return chatMessageRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    // ──────────────────────────────────────────────
    // 메시지 전송 + AI 응답 생성
    // ──────────────────────────────────────────────

    public ChatExchangeResponse sendMessage(String userId, SendMessageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        UserMemory memory = userMemoryService.getMemory(userId);

        // 1. 유저 메시지 저장 (별도 트랜잭션)
        ChatMessageResponse userMsg = saveUserMessage(userId, request.content());

        // 2. 히스토리 조회 (방금 저장한 메시지 포함)
        List<ChatMessage> history = chatMessageRepository
                .findByUserIdOrderByCreatedAtAsc(userId);

        // 3. AI 응답 생성
        String aiReply = chatAiClient.generateReply(history, user.getUsername(), memory);

        // 4. AI 응답 저장
        ChatMessage aiMessage = ChatMessage.create(userId, null, "ai", aiReply);
        chatMessageRepository.save(aiMessage);

        // 5. 메모리 비동기 업데이트 (3턴마다)
        updateMemoryAsync(userId, history, aiReply);

        log.info("채팅 완료 — userId: {}", userId);
        return ChatExchangeResponse.of(userMsg, ChatMessageResponse.from(aiMessage));
    }

    // ──────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────

    @Transactional
    public ChatMessageResponse saveUserMessage(String userId, String content) {
        ChatMessage userMessage = ChatMessage.create(userId, null, "user", content);
        chatMessageRepository.save(userMessage);
        return ChatMessageResponse.from(userMessage);
    }

    @Async("workerExecutor")
    public void updateMemoryAsync(String userId, List<ChatMessage> history, String aiReply) {
        try {
            chatAiClient.extractAndUpdateMemory(userId, history, aiReply, userMemoryService);
        } catch (Exception e) {
            log.error("채팅 메모리 업데이트 실패 — userId: {}, error: {}", userId, e.getMessage(), e);
        }
    }
}
