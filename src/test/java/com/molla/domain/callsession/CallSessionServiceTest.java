package com.molla.domain.callsession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.config.JwtProvider;
import com.molla.controller.dto.callsession.CallSessionResponse;
import com.molla.controller.dto.callsession.EndSessionRequest;
import com.molla.controller.dto.callsession.StartSessionRequest;
import com.molla.controller.dto.subscription.SubscriptionWithRemainingResponse;
import com.molla.domain.subscription.SubscriptionRepository;
import com.molla.domain.subscription.SubscriptionService;
import com.molla.domain.user.User;
import com.molla.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class CallSessionServiceTest {

    private final CallSessionRepository callSessionRepository = mock(CallSessionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final JwtProvider jwtProvider = mock(JwtProvider.class);

    private final CallSessionService callSessionService = new CallSessionService(
            callSessionRepository,
            userRepository,
            subscriptionRepository,
            subscriptionService,
            eventPublisher,
            objectMapper,
            jwtProvider,
            "wss://ai.example.com/call/ws"
    );

    @Test
    void startSessionCreatesUserWhenPhoneNumberDoesNotExist() {
        String phoneNumber = "01012345678";
        User newUser = User.createByPhone(phoneNumber);
        SubscriptionWithRemainingResponse subscription = new SubscriptionWithRemainingResponse(
                "sub-1",
                "premium",
                300,
                300,
                null,
                null,
                "active"
        );

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(newUser);
        when(callSessionRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);
        when(subscriptionService.getMySubscription(newUser.getId())).thenReturn(subscription);

        CallSessionResponse response = callSessionService.startSession(new StartSessionRequest(phoneNumber, "CA1234"));

        ArgumentCaptor<CallSession> sessionCaptor = ArgumentCaptor.forClass(CallSession.class);
        verify(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));
        verify(subscriptionService).ensureDemoPremiumSubscription(newUser.getId());
        verify(callSessionRepository).save(sessionCaptor.capture());

        CallSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getUserId()).isEqualTo(newUser.getId());
        assertThat(savedSession.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(savedSession.getUserStateAtCall()).isEqualTo("unregistered");
        assertThat(response.userStateAtCall()).isEqualTo("unregistered");
        assertThat(response.sessionType()).isEqualTo("level_test");
        assertThat(response.subscription()).isEqualTo(subscription);
    }

    @Test
    void startMySessionIssuesCallTokenAndWssUrlForAuthenticatedUser() {
        String phoneNumber = "01012345678";
        User existingUser = User.createByPhone(phoneNumber);
        SubscriptionWithRemainingResponse subscription = new SubscriptionWithRemainingResponse(
                "sub-client",
                "premium",
                300,
                300,
                null,
                null,
                "active"
        );

        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        when(callSessionRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);
        when(subscriptionService.getMySubscription(existingUser.getId())).thenReturn(subscription);
        when(jwtProvider.generateCallToken(eq(existingUser.getId()), anyString())).thenReturn("call-token");

        CallSessionResponse response = callSessionService.startMySession(existingUser.getId());

        ArgumentCaptor<CallSession> sessionCaptor = ArgumentCaptor.forClass(CallSession.class);
        verify(callSessionRepository).save(sessionCaptor.capture());
        CallSession savedSession = sessionCaptor.getValue();

        verify(jwtProvider).generateCallToken(existingUser.getId(), savedSession.getId());
        assertThat(response.callToken()).isEqualTo("call-token");
        assertThat(response.wssUrl()).isEqualTo("wss://ai.example.com/call/ws");
        assertThat(response.subscription()).isEqualTo(subscription);
    }

    @Test
    void startSessionReusesExistingUserWhenPhoneNumberExists() {
        String phoneNumber = "01012345678";
        User existingUser = User.createByPhone(phoneNumber);
        SubscriptionWithRemainingResponse subscription = new SubscriptionWithRemainingResponse(
                "sub-2",
                "premium",
                300,
                300,
                null,
                null,
                "active"
        );

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(existingUser));
        when(callSessionRepository.existsByPhoneNumber(phoneNumber)).thenReturn(true);
        when(subscriptionService.getMySubscription(existingUser.getId())).thenReturn(subscription);

        CallSessionResponse response = callSessionService.startSession(new StartSessionRequest(phoneNumber, "CA5678"));

        ArgumentCaptor<CallSession> sessionCaptor = ArgumentCaptor.forClass(CallSession.class);
        verify(callSessionRepository).save(sessionCaptor.capture());
        verify(userRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any(User.class));
        verify(subscriptionService, org.mockito.Mockito.never()).ensureDemoPremiumSubscription(org.mockito.ArgumentMatchers.any());

        CallSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getUserId()).isEqualTo(existingUser.getId());
        assertThat(savedSession.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(response.sessionType()).isEqualTo("practice");
        assertThat(response.subscription()).isEqualTo(subscription);
    }

    @Test
    void endSessionStoresProvidedDurationMinutesFromRequestAsSeconds() {
        User existingUser = User.createByPhone("01012345678");
        when(userRepository.findByPhoneNumber(existingUser.getPhoneNumber())).thenReturn(Optional.of(existingUser));
        when(callSessionRepository.existsByPhoneNumber(existingUser.getPhoneNumber())).thenReturn(false);
        when(subscriptionService.getMySubscription(existingUser.getId())).thenReturn(new SubscriptionWithRemainingResponse(
                "sub-3",
                "premium",
                300,
                300,
                null,
                null,
                "active"
        ));

        CallSessionResponse started = callSessionService.startSession(new StartSessionRequest(existingUser.getPhoneNumber(), "CA9999"));
        ArgumentCaptor<CallSession> startSessionCaptor = ArgumentCaptor.forClass(CallSession.class);
        verify(callSessionRepository).save(startSessionCaptor.capture());
        CallSession session = startSessionCaptor.getValue();

        when(callSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        EndSessionRequest request = new EndSessionRequest(
                "completed",
                3,
                List.of(new EndSessionRequest.TurnPayload(
                        1,
                        OffsetDateTime.parse("2026-05-20T12:00:01.123456+00:00"),
                        new EndSessionRequest.UserTurnPayload("hello", 16000, "calls/test/turn-1.wav"),
                        new EndSessionRequest.AssistantTurnPayload("hi", null, OffsetDateTime.parse("2026-05-20T12:00:02.234567+00:00"))
                ))
        );

        CallSessionResponse ended = callSessionService.endSession(session.getId(), request);

        assertThat(ended.durationSeconds()).isEqualTo(180);
        assertThat(session.getDurationSeconds()).isEqualTo(180);
        assertThat(session.getStatus()).isEqualTo("completed");
    }
}
