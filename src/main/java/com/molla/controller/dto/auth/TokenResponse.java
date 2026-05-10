package com.molla.controller.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT 토큰 응답")
public record TokenResponse(

        @Schema(description = "Access Token (만료: 1시간)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "Refresh Token (만료: 30일)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,

        @Schema(description = "신규 유저 여부. true면 앱 가입 플로우로 이동", example = "true")
        boolean isNewUser
) {
    public static TokenResponse of(String accessToken, String refreshToken, boolean isNewUser) {
        return new TokenResponse(accessToken, refreshToken, isNewUser);
    }
}
