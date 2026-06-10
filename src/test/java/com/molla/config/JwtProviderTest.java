package com.molla.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider(
            "molla-jwt-secret-key-must-be-at-least-256-bits-long-for-tests",
            3_600_000,
            2_592_000_000L
    );

    @Test
    void generateAccessTokenIncludesAccessClaims() {
        String token = jwtProvider.generateAccessToken("user-1", "01012345678");

        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getTokenType(token)).isEqualTo("access");
        assertThat(jwtProvider.getUserId(token)).isEqualTo("user-1");
        assertThat(jwtProvider.getPhoneNumber(token)).isEqualTo("01012345678");
    }
}
