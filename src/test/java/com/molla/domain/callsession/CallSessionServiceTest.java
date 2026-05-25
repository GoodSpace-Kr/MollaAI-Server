package com.molla.domain.callsession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.controller.dto.callsession.CallSessionResponse;
import com.molla.controller.dto.callsession.StartSessionRequest;
import com.molla.domain.subscription.SubscriptionRepository;
import com.molla.domain.user.User;
import com.molla.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallSessionServiceTest {

    private final CallSessionRepository callSessionRepository = mock(CallSessionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final CallSessionService callSessionService = new CallSessionService(
            callSessionRepository,
            userRepository,
            subscriptionRepository,
            eventPublisher,
            objectMapper
    );

    @Test
    void startSessionCreatesUserWhenPhoneNumberDoesNotExist() {
        String phoneNumber = "01012345678";
        User newUser = User.createByPhone(phoneNumber);

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenReturn(newUser);
        when(callSessionRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);

        CallSessionResponse response = callSessionService.startSession(new StartSessionRequest(phoneNumber, "CA1234"));

        ArgumentCaptor<CallSession> sessionCaptor = ArgumentCaptor.forClass(CallSession.class);
        verify(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));
        verify(callSessionRepository).save(sessionCaptor.capture());

        CallSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getUserId()).isEqualTo(newUser.getId());
        assertThat(savedSession.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(savedSession.getUserStateAtCall()).isEqualTo("unregistered");
        assertThat(response.userStateAtCall()).isEqualTo("unregistered");
        assertThat(response.sessionType()).isEqualTo("level_test");
    }

    @Test
    void startSessionReusesExistingUserWhenPhoneNumberExists() {
        String phoneNumber = "01012345678";
        User existingUser = User.createByPhone(phoneNumber);

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(existingUser));
        when(callSessionRepository.existsByPhoneNumber(phoneNumber)).thenReturn(true);

        CallSessionResponse response = callSessionService.startSession(new StartSessionRequest(phoneNumber, "CA5678"));

        ArgumentCaptor<CallSession> sessionCaptor = ArgumentCaptor.forClass(CallSession.class);
        verify(callSessionRepository).save(sessionCaptor.capture());

        CallSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getUserId()).isEqualTo(existingUser.getId());
        assertThat(savedSession.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(response.sessionType()).isEqualTo("practice");
    }
}
