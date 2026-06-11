package com.molla.controller;

import com.molla.common.response.ApiResponse;
import com.molla.config.JwtProvider;
import com.molla.controller.dto.auth.TokenResponse;
import com.molla.domain.subscription.SubscriptionService;
import com.molla.domain.user.User;
import com.molla.domain.user.UserRepository;
import com.molla.realtime.AgentConnectionRegistry;
import com.molla.realtime.JoinCallCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final AgentConnectionRegistry agentConnectionRegistry = mock(AgentConnectionRegistry.class);
    private final DevController devController = new DevController(
            userRepository,
            jwtProvider,
            subscriptionService,
            agentConnectionRegistry
    );

    @Test
    void devLoginCreatesDemoSubscriptionForNewUser() {
        User newUser = User.createByPhone("01012345678");
        when(userRepository.findByPhoneNumber("01012345678")).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(newUser);
        when(jwtProvider.generateAccessToken(newUser.getId(), newUser.getPhoneNumber())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(newUser.getId())).thenReturn("refresh-token");

        ResponseEntity<ApiResponse<TokenResponse>> response = devController.devLogin(
                new DevController.DevLoginRequest("01012345678")
        );

        verify(subscriptionService).ensureDemoPremiumSubscription(newUser.getId());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().accessToken()).isEqualTo("access-token");
        assertThat(response.getBody().data().isNewUser()).isTrue();
    }

    @Test
    void devLoginEnsuresDemoSubscriptionForExistingUser() {
        User existingUser = User.createByPhone("01098765432");
        when(userRepository.findByPhoneNumber("01098765432")).thenReturn(Optional.of(existingUser));
        when(jwtProvider.generateAccessToken(existingUser.getId(), existingUser.getPhoneNumber())).thenReturn("access-token");
        when(jwtProvider.generateRefreshToken(existingUser.getId())).thenReturn("refresh-token");

        ResponseEntity<ApiResponse<TokenResponse>> response = devController.devLogin(
                new DevController.DevLoginRequest("01098765432")
        );

        verify(subscriptionService).ensureDemoPremiumSubscription(existingUser.getId());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().accessToken()).isEqualTo("access-token");
        assertThat(response.getBody().data().isNewUser()).isTrue();
    }

    @Test
    void devAgentJoinCallSendsJoinCallToConnectedOrchestrator() {
        ResponseEntity<ApiResponse<DevController.DevAgentJoinCallResponse>> response = devController.devAgentJoinCall(
                new DevController.DevAgentJoinCallRequest(
                        "call-1",
                        "session-1",
                        "user-1",
                        "cf-session-1"
                )
        );

        ArgumentCaptor<JoinCallCommand> commandCaptor = ArgumentCaptor.forClass(JoinCallCommand.class);
        verify(agentConnectionRegistry).sendJoinCall(commandCaptor.capture());
        JoinCallCommand command = commandCaptor.getValue();

        assertThat(command.type()).isEqualTo("join_call");
        assertThat(command.callId()).isEqualTo("call-1");
        assertThat(command.sessionId()).isEqualTo("session-1");
        assertThat(command.userId()).isEqualTo("user-1");
        assertThat(command.realtime().sessionId()).isEqualTo("cf-session-1");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().sent()).isTrue();
    }
}
