package com.molla.domain.callsession;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;
import com.molla.controller.dto.callsession.CallSessionResponse;
import com.molla.controller.dto.callsession.EndSessionRequest;
import com.molla.controller.dto.callsession.StartSessionRequest;
import com.molla.controller.dto.callsession.WebrtcOfferRequest;
import com.molla.controller.dto.callsession.WebrtcOfferResponse;
import com.molla.controller.dto.subscription.SubscriptionWithRemainingResponse;
import com.molla.domain.subscription.SubscriptionRepository;
import com.molla.domain.subscription.SubscriptionService;
import com.molla.domain.user.User;
import com.molla.domain.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.realtime.CloudflareRealtimeClient;
import com.molla.realtime.IceServerProvider;
import com.molla.realtime.JoinCallCommand;
import com.molla.realtime.RealtimeSessionNegotiationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Service
public class CallSessionService {

    private final CallSessionRepository callSessionRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final CloudflareRealtimeClient cloudflareRealtimeClient;
    private final RealtimeSessionNegotiationService realtimeSessionNegotiationService;
    private final IceServerProvider iceServerProvider;

    public CallSessionService(
            CallSessionRepository callSessionRepository,
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionService subscriptionService,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            CloudflareRealtimeClient cloudflareRealtimeClient,
            RealtimeSessionNegotiationService realtimeSessionNegotiationService,
            IceServerProvider iceServerProvider
    ) {
        this.callSessionRepository = callSessionRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.cloudflareRealtimeClient = cloudflareRealtimeClient;
        this.realtimeSessionNegotiationService = realtimeSessionNegotiationService;
        this.iceServerProvider = iceServerProvider;
    }

    // ──────────────────────────────────────────────
    // 세션 시작 (내부 API)
    // ──────────────────────────────────────────────

    @Transactional
    public CallSessionResponse startSession(StartSessionRequest request) {
        User user = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseGet(() -> createUserWithDemoSubscription(request.phoneNumber()));
        String resolvedUserId = user.getId();
        String sessionType = resolveSessionType(request.phoneNumber());

        // 통화 시점의 유저 상태 스냅샷
        String userStateAtCall = resolveUserState(user);

        CallSession session = CallSession.create(
                resolvedUserId,
                request.phoneNumber(),
                request.callSid(),
                sessionType,
                userStateAtCall
        );

        callSessionRepository.save(session);
        SubscriptionWithRemainingResponse subscription = subscriptionService.getMySubscription(resolvedUserId);

        log.info("통화 세션 시작 — sessionId: {}, userId: {}, type: {}",
                session.getId(), session.getUserId(), sessionType);

        return CallSessionResponse.from(session, subscription);
    }

    // ──────────────────────────────────────────────
    // 세션 시작 (프론트 API)
    // ──────────────────────────────────────────────

    @Transactional
    public CallSessionResponse startMySession(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        String phoneNumber = user.getPhoneNumber();
        String sessionType = resolveSessionType(phoneNumber);
        String userStateAtCall = resolveUserState(user);

        CallSession session = CallSession.create(
                user.getId(),
                phoneNumber,
                null,
                sessionType,
                userStateAtCall
        );

        callSessionRepository.save(session);
        SubscriptionWithRemainingResponse subscription = subscriptionService.getMySubscription(user.getId());
        String realtimeSessionId = realtimeSessionNegotiationService.requestRealtimeSession(
                JoinCallCommand.of(session.getId(), session.getId(), user.getId(), "")
        );
        log.info("앱 통화 세션 시작 — sessionId: {}, userId: {}, type: {}",
                session.getId(), session.getUserId(), sessionType);

        return CallSessionResponse.from(session, subscription, realtimeSessionId, iceServerProvider.getIceServers());
    }

    // ──────────────────────────────────────────────
    // 세션 종료 (내부 API)
    // ──────────────────────────────────────────────

    @Transactional
    public CallSessionResponse endSession(String sessionId, EndSessionRequest request) {
        CallSession session = callSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CallSessionException(ErrorCode.SESSION_NOT_FOUND));
        return endSession(session, request, true);
    }

    @Transactional
    public CallSessionResponse endMySession(String sessionId, String userId, EndSessionRequest request) {
        String phoneNumber = getPhoneNumberByUserId(userId);
        CallSession session = callSessionRepository.findByIdAndPhoneNumber(sessionId, phoneNumber)
                .orElseThrow(() -> new CallSessionException(ErrorCode.SESSION_NOT_FOUND));
        return endSession(session, request, false);
    }

    private CallSessionResponse endSession(CallSession session, EndSessionRequest request, boolean requireTurnsForCompleted) {
        if (!session.isInProgress()) {
            throw new CallSessionException(ErrorCode.SESSION_ALREADY_ENDED);
        }

        String resolvedStatus = request != null ? request.resolvedStatus() : "completed";
        Integer requestedDurationMinutes = request != null ? request.durationMinutes() : null;
        List<CallSessionTurn> turns = request != null ? request.toCallSessionTurns() : List.of();

        if (requireTurnsForCompleted && "completed".equals(resolvedStatus) && turns.isEmpty()) {
            throw new CallSessionException(ErrorCode.INVALID_REQUEST, "completed 상태로 종료하려면 turns가 필요합니다.");
        }

        if (requestedDurationMinutes != null && requestedDurationMinutes < 0) {
            throw new CallSessionException(ErrorCode.INVALID_REQUEST, "durationMinutes는 0 이상이어야 합니다.");
        }

        if (!turns.isEmpty()) {
            try {
                session.updateTurnsJson(objectMapper.writeValueAsString(turns));
            } catch (Exception e) {
                throw new CallSessionException(ErrorCode.INTERNAL_SERVER_ERROR, "turns 저장 직렬화에 실패했습니다.");
            }
        }

        if ("failed".equals(resolvedStatus)) {
            session.fail();
        } else {
            session.end(toDurationSeconds(requestedDurationMinutes));
        }

        // 통화 종료 후 비동기 워커 트리거 (Spring Event)
        // 리포트 생성 → Qdrant upsert 순으로 처리
        if ("completed".equals(session.getStatus()) && !turns.isEmpty()) {
            eventPublisher.publishEvent(new SessionEndedEvent(session.getId(), session.getUserId(), session.isLevelTest()));
        }

        log.info("통화 세션 종료 — sessionId: {}, duration: {}초, status: {}",
                session.getId(), session.getDurationSeconds(), session.getStatus());

        return CallSessionResponse.from(session);
    }

    // ──────────────────────────────────────────────
    // 통화 목록 조회 (프론트용)
    // ──────────────────────────────────────────────

    public List<CallSessionResponse> getMySessions(String userId) {
        String phoneNumber = getPhoneNumberByUserId(userId);
        return callSessionRepository.findByPhoneNumberOrderByStartedAtDesc(phoneNumber)
                .stream()
                .map(CallSessionResponse::from)
                .toList();
    }

    // ──────────────────────────────────────────────
    // 통화 상세 조회 (프론트용)
    // ──────────────────────────────────────────────

    public CallSessionResponse getSession(String sessionId, String userId) {
        String phoneNumber = getPhoneNumberByUserId(userId);
        CallSession session = callSessionRepository.findByIdAndPhoneNumber(sessionId, phoneNumber)
                .orElseThrow(() -> new CallSessionException(ErrorCode.SESSION_NOT_FOUND));
        return CallSessionResponse.from(session);
    }

    public WebrtcOfferResponse submitWebrtcOffer(String sessionId, String userId, WebrtcOfferRequest request) {
        String phoneNumber = getPhoneNumberByUserId(userId);
        callSessionRepository.findByIdAndPhoneNumber(sessionId, phoneNumber)
                .orElseThrow(() -> new CallSessionException(ErrorCode.SESSION_NOT_FOUND));
        return WebrtcOfferResponse.fromCloudflare(
                cloudflareRealtimeClient.addTracks(request.realtimeSessionId(), request.toCloudflarePayload())
        );
    }

    // ──────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────

    private String resolveUserState(User user) {
        if (user == null) return "unregistered";
        if (!user.isRegistered()) return "unregistered";
        if (subscriptionRepository.existsActiveByUserId(user.getId())) return "subscribed";
        return "registered";
    }

    private String resolveSessionType(String phoneNumber) {
        return callSessionRepository.existsByPhoneNumber(phoneNumber) ? "practice" : "level_test";
    }

    private String getPhoneNumberByUserId(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND))
                .getPhoneNumber();
    }

    private User createUserWithDemoSubscription(String phoneNumber) {
        User savedUser = userRepository.save(User.createByPhone(phoneNumber));
        subscriptionService.ensureDemoPremiumSubscription(savedUser.getId());
        return savedUser;
    }

    private Integer toDurationSeconds(Integer durationMinutes) {
        if (durationMinutes == null) {
            return null;
        }
        return durationMinutes * 60;
    }

}
