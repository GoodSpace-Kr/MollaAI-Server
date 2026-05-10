package com.molla.controller.dto.subscription;

import com.molla.domain.subscription.Subscription;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "구독 정보 응답")
public record SubscriptionResponse(

        @Schema(description = "구독 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "플랜 타입 (free / premium)", example = "premium")
        String planType,

        @Schema(description = "하루 통화 가능 분", example = "60")
        int dailyLimitMinutes,

        @Schema(description = "구독 시작 일시")
        LocalDateTime startedAt,

        @Schema(description = "구독 만료 일시 (null이면 무기한)")
        LocalDateTime expiresAt,

        @Schema(description = "구독 상태 (active / expired / cancelled)", example = "active")
        String status
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getPlanType(),
                subscription.getDailyLimitMinutes(),
                subscription.getStartedAt(),
                subscription.getExpiresAt(),
                subscription.getStatus()
        );
    }
}
