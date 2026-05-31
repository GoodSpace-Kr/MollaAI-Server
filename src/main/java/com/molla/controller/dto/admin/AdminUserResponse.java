package com.molla.controller.dto.admin;

import com.molla.domain.user.User;
import java.time.LocalDateTime;

public record AdminUserResponse(
        String id,
        String username,
        String phoneNumber,
        String status,
        String englishLevel,
        boolean registered,
        LocalDateTime registeredAt,
        LocalDateTime firstCallAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getEnglishLevel(),
                user.isRegistered(),
                user.getRegisteredAt(),
                user.getFirstCallAt()
        );
    }
}
