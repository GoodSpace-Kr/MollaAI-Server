package com.molla.controller.dto.admin;

import com.molla.domain.subscription.Subscription;
import com.molla.domain.user.User;
import java.time.LocalDateTime;

public record AdminUserDetailResponse(
        String id,
        String username,
        String phoneNumber,
        String status,
        String englishLevel,
        boolean registered,
        LocalDateTime registeredAt,
        LocalDateTime firstCallAt,
        int totalSessions,
        int totalDurationMinutes,
        String planType,
        LocalDateTime subscriptionExpiresAt
) {
    public static AdminUserDetailResponse of(
            User user,
            int totalSessions,
            int totalDurationSeconds,
            Subscription subscription
    ) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getEnglishLevel(),
                user.isRegistered(),
                user.getRegisteredAt(),
                user.getFirstCallAt(),
                totalSessions,
                totalDurationSeconds / 60,
                subscription != null ? subscription.getPlanType() : null,
                subscription != null ? subscription.getExpiresAt() : null
        );
    }
}
