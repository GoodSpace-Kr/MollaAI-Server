package com.molla.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    // ──────────────────────────────────────────────
    // 생성
    // ──────────────────────────────────────────────

    /** Access Token 생성 */
    public String generateAccessToken(String userId, String phoneNumber) {
        return buildToken(userId, phoneNumber, accessTokenExpirationMs, "access");
    }

    /** Refresh Token 생성 */
    public String generateRefreshToken(String userId) {
        return buildToken(userId, null, refreshTokenExpirationMs, "refresh");
    }

    private String buildToken(String userId, String phoneNumber, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(userId)
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(expiry);

        if (phoneNumber != null) {
            builder.claim("phone", phoneNumber);
        }

        return builder.signWith(secretKey).compact();
    }

    // ──────────────────────────────────────────────
    // 검증 & 파싱
    // ──────────────────────────────────────────────

    /** 토큰 유효성 검사 (서명 + 만료) */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT 만료: {}", e.getMessage());
        } catch (JwtException e) {
            log.debug("JWT 검증 실패: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT 빈 값");
        }
        return false;
    }

    /** userId(subject) 추출 */
    public String getUserId(String token) {
        return getClaims(token).getSubject();
    }

    /** phoneNumber 추출 (Access Token 전용) */
    public String getPhoneNumber(String token) {
        return getClaims(token).get("phone", String.class);
    }

    /** 토큰 타입 확인 ("access" | "refresh") */
    public String getTokenType(String token) {
        return getClaims(token).get("type", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
