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

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final DailyUsageCalculator dailyUsageCalculator;

    // ──────────────────────────────────────────────
    // 내 구독 조회
    // ──────────────────────────────────────────────

    @Transactional  // expireSubscription() Dirty Checking 반영을 위해 필요
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

        int dailyLimit = request.dailyLimitMinutes() != null ? request.dailyLimitMinutes() : 30;
        LocalDateTime expiresAt = request.durationDays() != null
                ? LocalDateTime.now().plusDays(request.durationDays())
                : null;

        Subscription subscription = Subscription.create(userId, request.planType(), dailyLimit, expiresAt);
        subscriptionRepository.save(subscription);

        log.info("구독 생성 완료 — userId: {}, planType: {}", userId, request.planType());
        return SubscriptionResponse.from(subscription);
    }

    // ──────────────────────────────────────────────
    // 통화 분 차감 검증 (내부 API에서 호출)
    // ──────────────────────────────────────────────

    @Transactional
    public void deductMinutes(String userId, int usedMinutes) {
        Subscription subscription = subscriptionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new SubscriptionException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

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
