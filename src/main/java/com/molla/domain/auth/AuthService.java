package com.molla.domain.auth;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;
import com.molla.config.JwtProvider;
import com.molla.controller.dto.auth.AccessTokenResponse;
import com.molla.controller.dto.auth.TokenResponse;
import com.molla.domain.user.User;
import com.molla.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int CODE_EXPIRATION_MINUTES = 5;

    private final AuthCodeRepository authCodeRepository;
    private final UserRepository userRepository;
    private final SensClient sensClient;
    private final JwtProvider jwtProvider;

    // ──────────────────────────────────────────────
    // 인증번호 발송
    // ──────────────────────────────────────────────

    @Transactional
    public void sendVerificationCode(String phoneNumber) {
        // 기존 코드 삭제 (재발송 처리)
        authCodeRepository.deleteAllByPhoneNumber(phoneNumber);

        String code = generateCode();

        AuthCode authCode = AuthCode.builder()
                .phoneNumber(phoneNumber)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES))
                .build();

        authCodeRepository.save(authCode);

        sensClient.sendSms(phoneNumber, "[Molla] 인증번호: " + code + " (5분 이내 입력해주세요.)");

        log.info("인증번호 발송 완료 — phoneNumber: {}", phoneNumber);
    }

    // ──────────────────────────────────────────────
    // 인증번호 확인 + JWT 발급
    // ──────────────────────────────────────────────

    @Transactional
    public TokenResponse verifyCode(String phoneNumber, String code) {
        AuthCode authCode = authCodeRepository
                .findLatestUnverifiedByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new AuthException(ErrorCode.VERIFICATION_CODE_NOT_FOUND));

        if (authCode.isExpired()) {
            throw new AuthException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!authCode.getCode().equals(code)) {
            throw new AuthException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        authCode.markVerified();

        boolean isNewUser = !userRepository.existsByPhoneNumber(phoneNumber);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> userRepository.save(User.createByPhone(phoneNumber)));

        String accessToken = jwtProvider.generateAccessToken(user.getId(), phoneNumber);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        // Dirty Checking으로 Refresh Token 저장
        user.updateRefreshToken(refreshToken, LocalDateTime.now().plusDays(30));

        log.info("인증 완료 — userId: {}, isNewUser: {}", user.getId(), isNewUser);
        return TokenResponse.of(accessToken, refreshToken, isNewUser);
    }

    // ──────────────────────────────────────────────
    // Access Token 재발급
    // ──────────────────────────────────────────────

    @Transactional
    public AccessTokenResponse refreshToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }

        if (!"refresh".equals(jwtProvider.getTokenType(refreshToken))) {
            throw new AuthException(ErrorCode.INVALID_TOKEN);
        }

        String userId = jwtProvider.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new AuthException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        if (user.getTokenExpiresAt() == null || LocalDateTime.now().isAfter(user.getTokenExpiresAt())) {
            throw new AuthException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        String newAccessToken = jwtProvider.generateAccessToken(userId, user.getPhoneNumber());

        log.info("Access Token 재발급 — userId: {}", userId);
        return AccessTokenResponse.of(newAccessToken);
    }

    // ──────────────────────────────────────────────

    private String generateCode() {
        return String.valueOf(new SecureRandom().nextInt(900000) + 100000);
    }
}
