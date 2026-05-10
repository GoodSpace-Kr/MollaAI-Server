package com.molla.domain.conversationturn;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;
import com.molla.controller.dto.conversationturn.ConversationTurnResponse;
import com.molla.controller.dto.conversationturn.SaveTurnRequest;
import com.molla.domain.callsession.CallSession;
import com.molla.domain.callsession.CallSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationTurnService {

    private final ConversationTurnRepository conversationTurnRepository;
    private final CallSessionRepository callSessionRepository;

    // ──────────────────────────────────────────────
    // 발화 기록 저장 (내부 API)
    // ──────────────────────────────────────────────

    public ConversationTurnResponse saveTurn(SaveTurnRequest request) {
        // 세션 존재 여부 확인
        CallSession session = callSessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new GlobalException(ErrorCode.SESSION_NOT_FOUND));

        // 이미 종료된 세션에는 발화 저장 불가
        if (!session.isInProgress()) {
            throw new GlobalException(ErrorCode.SESSION_ALREADY_ENDED);
        }

        // 다음 sequence_order 계산 (현재 저장된 발화 수 + 1)
        int nextOrder = conversationTurnRepository.countBySessionId(request.sessionId()) + 1;

        ConversationTurn turn = ConversationTurn.create(
                request.sessionId(),
                request.speaker(),
                request.content(),
                request.confidenceScore(),
                request.audioUrl(),
                nextOrder
        );

        conversationTurnRepository.save(turn);

        log.debug("발화 저장 — sessionId: {}, speaker: {}, order: {}",
                request.sessionId(), request.speaker(), nextOrder);

        return ConversationTurnResponse.from(turn);
    }

    // ──────────────────────────────────────────────
    // 세션 발화 목록 조회 (프론트용)
    // ──────────────────────────────────────────────

    public List<ConversationTurnResponse> getTurnsBySession(String sessionId, String userId) {
        // 본인 세션인지 확인
        callSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.SESSION_NOT_FOUND));

        return conversationTurnRepository.findBySessionIdOrderBySequenceOrderAsc(sessionId)
                .stream()
                .map(ConversationTurnResponse::from)
                .toList();
    }
}
