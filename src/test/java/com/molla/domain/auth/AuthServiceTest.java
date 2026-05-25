package com.molla.domain.auth;

import com.molla.config.JwtProvider;
import com.molla.controller.dto.auth.TokenResponse;
import com.molla.domain.subscription.SubscriptionService;
import com.molla.domain.user.User;
import com.molla.domain.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final AuthCodeRepository authCodeRepository = mock(AuthCodeRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SensClient sensClient = mock(SensClient.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);

    private final AuthService authService = new AuthService(
            authCodeRepository,
            userRepository,
            sensClient,
            jwtProvider,
            subscriptionService
    );

    @Test
    void verifyCodeCreatesUserAndDemoSubscriptionForNewPhoneNumber() {
        String phoneNumber = "01012345678";
        AuthCode authCode = AuthCode.builder()
                .phoneNumber(phoneNumber)
                .code("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        User newUser = User.createByPhone(phoneNumber);

        when(authCodeRepository.findLatestUnverifiedByPhoneNumber(phoneNumber)).thenReturn(Optional.of(authCode));
        when(userRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(newUser);
        when(jwtProvider.generateAccessToken(newUser.getId(), phoneNumber)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(newUser.getId())).thenReturn("refresh-token");

        TokenResponse response = authService.verifyCode(phoneNumber, "123456");

        verify(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));
        verify(subscriptionService).ensureDemoPremiumSubscription(newUser.getId());
        assertThat(response.isNewUser()).isTrue();
    }

    @Test
    void verifyCodeReusesExistingUserWithoutCreatingSubscription() {
        String phoneNumber = "01012345678";
        AuthCode authCode = AuthCode.builder()
                .phoneNumber(phoneNumber)
                .code("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        User existingUser = User.createByPhone(phoneNumber);
        existingUser.register("existing-user");

        when(authCodeRepository.findLatestUnverifiedByPhoneNumber(phoneNumber)).thenReturn(Optional.of(authCode));
        when(userRepository.existsByPhoneNumber(phoneNumber)).thenReturn(true);
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(existingUser));
        when(jwtProvider.generateAccessToken(existingUser.getId(), phoneNumber)).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(existingUser.getId())).thenReturn("refresh-token");

        TokenResponse response = authService.verifyCode(phoneNumber, "123456");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
        verify(subscriptionService, never()).ensureDemoPremiumSubscription(org.mockito.ArgumentMatchers.any());
        assertThat(response.isNewUser()).isFalse();
    }
}
