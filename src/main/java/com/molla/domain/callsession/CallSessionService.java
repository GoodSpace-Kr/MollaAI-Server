package com.molla.domain.callsession;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;
import com.molla.controller.dto.callsession.CallSessionResponse;
import com.molla.controller.dto.callsession.EndSessionRequest;
import com.molla.controller.dto.callsession.StartSessionRequest;
import com.molla.domain.subscription.SubscriptionRepository;
import com.molla.domain.user.User;
import com.molla.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallSessionService {

    private final CallSessionRepository callSessionRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ──────────────────────────────────────────────
    // 세션 시작 (내부 API)
    // ──────────────────────────────────────────────

    @Transactional
    public CallSessionResponse startSession(StartSessionRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 통화 시점의 유저 상태 스냅샷
        String userStateAtCall = resolveUserState(user, request.userId());

        // practice 타입이면 구독 여부 확인
        if ("practice".equals(request.sessionType())) {
            boolean hasActiveSubscription = subscriptionRepository.existsActiveByUserId(request.userId());
            if (!hasActiveSubscription) {
                throw new GlobalException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
            }
        }

        CallSession session = CallSession.create(
                request.userId(),
                request.callSid(),
                request.aiWsSessionId(),
                request.sessionType(),
                userStateAtCall,
                request.topic()
        );

        callSessionRepository.save(session);

        log.info("통화 세션 시작 — sessionId: {}, userId: {}, type: {}",
                session.getId(), request.userId(), request.sessionType());

        return CallSessionResponse.from(session);
    }

    // ──────────────────────────────────────────────
    // 세션 종료 (내부 API)
    // ──────────────────────────────────────────────

    @Transactional
    public CallSessionResponse endSession(String sessionId, EndSessionRequest request) {
        CallSession session = callSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CallSessionException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.isInProgress()) {
            throw new CallSessionException(ErrorCode.SESSION_ALREADY_ENDED);
        }

        String resolvedStatus = request != null ? request.resolvedStatus() : "completed";

        if ("failed".equals(resolvedStatus)) {
            session.fail();
        } else {
            session.end();
        }

        // 통화 종료 후 비동기 워커 트리거 (Spring Event)
        // 리포트 생성 → user_memories 갱신 → Qdrant upsert 순으로 처리
        if ("completed".equals(session.getStatus())) {
            eventPublisher.publishEvent(new SessionEndedEvent(session.getId(), session.getUserId(), session.isLevelTest()));
        }

        log.info("통화 세션 종료 — sessionId: {}, duration: {}초, status: {}",
                sessionId, session.getDurationSeconds(), session.getStatus());

        return CallSessionResponse.from(session);
    }

    // ──────────────────────────────────────────────
    // 통화 목록 조회 (프론트용)
    // ──────────────────────────────────────────────

    public List<CallSessionResponse> getMySessions(String userId) {
        return callSessionRepository.findByUserIdOrderByStartedAtDesc(userId)
                .stream()
                .map(CallSessionResponse::from)
                .toList();
    }

    // ──────────────────────────────────────────────
    // 통화 상세 조회 (프론트용)
    // ──────────────────────────────────────────────

    public CallSessionResponse getSession(String sessionId, String userId) {
        CallSession session = callSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new CallSessionException(ErrorCode.SESSION_NOT_FOUND));
        return CallSessionResponse.from(session);
    }

    // ──────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────

    private String resolveUserState(User user, String userId) {
        if (!user.isRegistered()) return "unregistered";
        if (subscriptionRepository.existsActiveByUserId(userId)) return "subscribed";
        return "registered";
    }
}
