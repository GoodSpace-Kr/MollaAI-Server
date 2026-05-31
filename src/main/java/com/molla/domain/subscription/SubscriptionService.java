package com.molla.domain.subscription;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;
import com.molla.controller.dto.subscription.CreateSubscriptionRequest;
import com.molla.controller.dto.subscription.SubscriptionResponse;
import com.molla.controller.dto.subscription.SubscriptionWithRemainingResponse;
import com.molla.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String DEMO_DEFAULT_PLAN_TYPE = "free";

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final DailyUsageCalculator dailyUsageCalculator;

    // ──────────────────────────────────────────────
    // 플랜별 설정
    // ──────────────────────────────────────────────

    public static int getDailyLimitByPlan(String planType) {
        return switch (planType) {
            case "premium"      -> 300;
            case "max"          -> 500;
            case "professional" -> 99999;
            default             -> 30;   // free
        };
    }

    public static int getPriceByPlan(String planType) {
        return switch (planType) {
            case "premium"      -> 9900;
            case "max"          -> 12900;
            case "professional" -> 19900;
            default             -> 0;    // free
        };
    }

    public static boolean isUnlimited(String planType) {
        return "professional".equals(planType);
    }

    // ──────────────────────────────────────────────
    // 내 구독 조회
    // ──────────────────────────────────────────────

    @Transactional
    public SubscriptionWithRemainingResponse getMySubscription(String userId) {
        Subscription subscription = subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (subscription.isExpired()) {
            expireSubscription(subscription);
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
        }

        int remainingMinutes = getRemainingMinutes(userId, subscription);
        return SubscriptionWithRemainingResponse.of(subscription, remainingMinutes);
    }

    // ──────────────────────────────────────────────
    // 구독 생성
    // ──────────────────────────────────────────────

    @Transactional
    public SubscriptionResponse createSubscription(String userId, CreateSubscriptionRequest request) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (subscriptionRepository.existsActiveByUserId(userId)) {
            throw new SubscriptionException(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        // 플랜에서 자동 결정
        int dailyLimit = getDailyLimitByPlan(request.planType());
        LocalDateTime expiresAt = request.durationDays() != null
                ? LocalDateTime.now().plusDays(request.durationDays())
                : LocalDateTime.now().plusDays(30);

        Subscription subscription = Subscription.create(userId, request.planType(), dailyLimit, expiresAt);
        subscriptionRepository.save(subscription);

        log.info("구독 생성 완료 — userId: {}, planType: {}, dailyLimit: {}분",
                userId, request.planType(), dailyLimit);
        return SubscriptionResponse.from(subscription);
    }

    @Transactional
    public void ensureDemoPremiumSubscription(String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (subscriptionRepository.existsActiveByUserId(userId)) {
            log.info("기존 활성 구독 재사용 — userId: {}", userId);
            return;
        }

        int dailyLimit = getDailyLimitByPlan(DEMO_DEFAULT_PLAN_TYPE);
        Subscription subscription = Subscription.create(
                userId,
                DEMO_DEFAULT_PLAN_TYPE,
                dailyLimit,
                null
        );
        subscriptionRepository.save(subscription);

        log.info("데모 기본 구독 생성 완료 — userId: {}, planType: {}", userId, DEMO_DEFAULT_PLAN_TYPE);
    }

    // ──────────────────────────────────────────────
    // 통화 분 차감 검증
    // ──────────────────────────────────────────────

    @Transactional
    public void deductMinutes(String userId, int usedMinutes) {
        Subscription subscription = subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // professional은 무제한 — 차감 검증 스킵
        if (isUnlimited(subscription.getPlanType())) {
            log.info("무제한 플랜 — 차감 검증 스킵, userId: {}", userId);
            return;
        }

        int remaining = getRemainingMinutes(userId, subscription);
        if (remaining < usedMinutes) {
            throw new SubscriptionException(ErrorCode.DAILY_LIMIT_EXCEEDED);
        }

        log.info("통화 분 차감 검증 완료 — userId: {}, usedMinutes: {}, remaining: {}",
                userId, usedMinutes, remaining);
    }

    // ──────────────────────────────────────────────
    // 잔여 통화 가능 분 조회
    // ──────────────────────────────────────────────

    public int getRemainingMinutes(String userId) {
        Subscription subscription = subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
        return getRemainingMinutes(userId, subscription);
    }

    // ──────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────

    private int getRemainingMinutes(String userId, Subscription subscription) {
        // professional은 무제한
        if (isUnlimited(subscription.getPlanType())) {
            return 99999;
        }
        int usedMinutesToday = dailyUsageCalculator.calculateTodayUsedMinutes(userId);
        int remaining = subscription.getDailyLimitMinutes() - usedMinutesToday;
        return Math.max(remaining, 0);
    }

    @Transactional
    public void expireSubscription(Subscription subscription) {
        subscription.expire();
        log.info("구독 만료 처리 — subscriptionId: {}", subscription.getId());
    }
}
