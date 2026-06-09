package com.molla.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider(
            "molla-jwt-secret-key-must-be-at-least-256-bits-long-for-tests",
            "molla-call-jwt-secret-key-must-be-at-least-256-bits-long-for-tests",
            3_600_000,
            2_592_000_000L,
            300_000
    );

    @Test
    void generateCallTokenIncludesSessionClaims() {
        String token = jwtProvider.generateCallToken("user-1", "session-1");

        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getTokenType(token)).isEqualTo("call");
        assertThat(jwtProvider.getUserId(token)).isEqualTo("user-1");
        assertThat(jwtProvider.getSessionId(token)).isEqualTo("session-1");
        assertThat(jwtProvider.getScope(token)).isEqualTo("call:connect");
        assertThat(jwtProvider.getAudience(token)).isEqualTo("molla-orchestrator");
    }
}
