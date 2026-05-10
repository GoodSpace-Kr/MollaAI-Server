package com.molla.controller.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Access Token 재발급 요청")
public record RefreshTokenRequest(

        @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "Refresh Token을 입력해주세요.")
        String refreshToken
) {}
