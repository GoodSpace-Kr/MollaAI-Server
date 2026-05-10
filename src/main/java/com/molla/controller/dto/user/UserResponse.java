package com.molla.controller.dto.user;

import com.molla.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "유저 정보 응답")
public record UserResponse(

        @Schema(description = "유저 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "전화번호", example = "01012345678")
        String phoneNumber,

        @Schema(description = "닉네임", example = "홍길동")
        String username,

        @Schema(description = "가입 완료 여부", example = "true")
        boolean isRegistered,

        @Schema(description = "영어 레벨 (beginner / intermediate / advanced)", example = "beginner")
        String englishLevel,

        @Schema(description = "계정 상태 (active / suspended / withdrawn)", example = "active")
        String status,

        @Schema(description = "최초 통화 일시")
        LocalDateTime firstCallAt,

        @Schema(description = "가입 완료 일시")
        LocalDateTime registeredAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getUsername(),
                user.isRegistered(),
                user.getEnglishLevel(),
                user.getStatus(),
                user.getFirstCallAt(),
                user.getRegisteredAt()
        );
    }
}
