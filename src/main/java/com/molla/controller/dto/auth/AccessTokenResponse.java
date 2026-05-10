package com.molla.controller.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access Token 재발급 응답")
public record AccessTokenResponse(

        @Schema(description = "새로 발급된 Access Token (만료: 1시간)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken
) {
    public static AccessTokenResponse of(String accessToken) {
        return new AccessTokenResponse(accessToken);
    }
}
