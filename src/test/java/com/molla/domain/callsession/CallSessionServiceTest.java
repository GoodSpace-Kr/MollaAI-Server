package com.molla.domain.callsession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.controller.dto.callsession.CallSessionResponse;
import com.molla.controller.dto.callsession.EndSessionRequest;
import com.molla.controller.dto.callsession.StartSessionRequest;
import com.molla.controller.dto.callsession.WebrtcOfferRequest;
import com.molla.controller.dto.callsession.WebrtcOfferResponse;
import com.molla.controller.dto.callsession.WebrtcSubscribeRequest;
import com.molla.controller.dto.subscription.SubscriptionWithRemainingResponse;
import com.molla.domain.subscription.SubscriptionRepository;
import com.molla.domain.subscription.SubscriptionService;
import com.molla.domain.user.User;
import com.molla.domain.user.UserRepository;
import com.molla.realtime.AgentConnectionRegistry;
import com.molla.realtime.CloudflareRealtimeClient;
import com.molla.realtime.IceServerProvider;
import com.molla.realtime.JoinCallCommand;
import com.molla.realtime.RealtimeSessionNegotiationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.WebSocketSession;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallSessionServiceTest {

    private final CallSessionRepository callSessionRepository = mock(CallSessionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final CloudflareRealtimeClient cloudflareRealtimeClient = mock(CloudflareRealtimeClient.class);
    private final RealtimeSessionNegotiationService realtimeSessionNegotiationService = mock(RealtimeSessionNegotiationService.class);
    private final IceServerProvider iceServerProvider = mock(IceServerProvider.class);
    private final AgentConnectionRegistry agentConnectionRegistry = mock(AgentConnectionRegistry.class);

    private final CallSessionService callSessionService = new CallSessionService(
            callSessionRepository,
            userRepository,
            subscriptionRepository,
            subscriptionService,
            eventPublisher,
            objectMapper,
            cloudflareRealtimeClient,
            realtimeSessionNegotiationService,
            iceServerProvider,
            agentConnectionRegistry
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
    void startMySessionCreatesCloudflareSessionAndDispatchesJoinCallToConnectedAgent() {
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
        when(realtimeSessionNegotiationService.requestRealtimeSession(org.mockito.ArgumentMatchers.any(JoinCallCommand.class)))
                .thenReturn("cf-session-1");
        when(iceServerProvider.getIceServers()).thenReturn(List.of(
                Map.of("urls", List.of("stun:stun.cloudflare.com:3478")),
                Map.of(
                        "urls", List.of("turn:turn.cloudflare.com:3478?transport=udp"),
                        "username", "turn-user",
                        "credential", "turn-credential"
                )
        ));

        CallSessionResponse response = callSessionService.startMySession(existingUser.getId());

        ArgumentCaptor<CallSession> sessionCaptor = ArgumentCaptor.forClass(CallSession.class);
        verify(callSessionRepository).save(sessionCaptor.capture());
        CallSession savedSession = sessionCaptor.getValue();

        verify(realtimeSessionNegotiationService).requestRealtimeSession(
                org.mockito.ArgumentMatchers.argThat(command ->
                        command.callId().equals(savedSession.getId())
                                && command.sessionId().equals(savedSession.getId())
                                && command.userId().equals(existingUser.getId())
                                && command.realtime().sessionId().isEmpty()
                )
        );
        assertThat(response.agentRealtimeSessionId()).isEqualTo("cf-session-1");
        assertThat(response.iceServers()).hasSize(2);
        assertThat(response.iceServers().get(1)).containsEntry("username", "turn-user");
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
    void submitWebrtcOfferCreatesAppCloudflareSessionWithoutSubscribingBeforeAppConnects() {
        User existingUser = User.createByPhone("01012345678");
        CallSession session = CallSession.create(
                existingUser.getId(),
                existingUser.getPhoneNumber(),
                null,
                "practice",
                "subscribed"
        );
        WebrtcOfferRequest request = new WebrtcOfferRequest(
                "cf-session-1",
                Map.of("type", "offer", "sdp", "local-sdp"),
                List.of(Map.of("trackName", "user_audio"))
        );
        Map<String, Object> cloudflareSession = Map.of(
                "sessionId", "cf-app-session-1"
        );
        Map<String, Object> publishResponse = Map.of(
                "sessionDescription", Map.of("type", "answer", "sdp", "remote-sdp"),
                "tracks", List.of(Map.of("trackName", "user_audio"))
        );

        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        when(callSessionRepository.findByIdAndPhoneNumber(session.getId(), existingUser.getPhoneNumber()))
                .thenReturn(Optional.of(session));
        when(cloudflareRealtimeClient.createSession(request.toCloudflarePayload())).thenReturn(cloudflareSession);
        when(cloudflareRealtimeClient.addTracks("cf-app-session-1", request.toCloudflarePayload()))
                .thenReturn(publishResponse);
        WebrtcOfferResponse response = callSessionService.submitWebrtcOffer(session.getId(), existingUser.getId(), request);

        verify(cloudflareRealtimeClient).createSession(request.toCloudflarePayload());
        verify(cloudflareRealtimeClient).addTracks("cf-app-session-1", request.toCloudflarePayload());
        assertThat(response.appRealtimeSessionId()).isEqualTo("cf-app-session-1");
        assertThat(response.sessionDescription()).containsEntry("type", "answer");
        assertThat(response.sessionDescription()).containsEntry("sdp", "remote-sdp");
        assertThat(response.tracks()).hasSize(1);
    }

    @Test
    void subscribeWebrtcAudioAddsAppTrackToAgentSessionAfterAppConnects() {
        User existingUser = User.createByPhone("01012345678");
        CallSession session = CallSession.create(
                existingUser.getId(),
                existingUser.getPhoneNumber(),
                null,
                "practice",
                "subscribed"
        );
        WebrtcSubscribeRequest request = new WebrtcSubscribeRequest(
                "cf-session-1",
                "cf-app-session-1",
                null
        );

        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        when(callSessionRepository.findByIdAndPhoneNumber(session.getId(), existingUser.getPhoneNumber()))
                .thenReturn(Optional.of(session));
        when(cloudflareRealtimeClient.addTracks("cf-session-1", Map.of(
                "tracks", List.of(Map.of(
                        "location", "remote",
                        "sessionId", "cf-app-session-1",
                        "trackName", "user_audio"
                ))
        ))).thenReturn(Map.of(
                "requiresImmediateRenegotiation", true,
                "tracks", List.of()
        ));
        WebSocketSession agentSession = mock(WebSocketSession.class);
        when(agentConnectionRegistry.current()).thenReturn(Optional.of(agentSession));

        callSessionService.subscribeWebrtcAudio(session.getId(), existingUser.getId(), request);

        verify(cloudflareRealtimeClient).addTracks("cf-session-1", Map.of(
                "tracks", List.of(Map.of(
                        "location", "remote",
                        "sessionId", "cf-app-session-1",
                        "trackName", "user_audio"
                ))
        ));
        verify(agentConnectionRegistry).send(agentSession, Map.of(
                "type", "webrtc_renegotiate",
                "callId", session.getId(),
                "realtimeSessionId", "cf-session-1"
        ));
    }

    @Test
    void submitWebrtcOfferAppendsEndOfCandidatesToCloudflareAnswer() {
        User existingUser = User.createByPhone("01012345678");
        CallSession session = CallSession.create(
                existingUser.getId(),
                existingUser.getPhoneNumber(),
                null,
                "practice",
                "subscribed"
        );
        WebrtcOfferRequest request = new WebrtcOfferRequest(
                "cf-session-1",
                Map.of("type", "offer", "sdp", "local-sdp"),
                List.of(Map.of("location", "local", "mid", "0", "trackName", "user_audio"))
        );
        String cloudflareAnswerSdp = """
                v=0\r
                m=audio 1473 UDP/TLS/RTP/SAVPF 96\r
                a=mid:0\r
                a=candidate:513273236 1 udp 2130706431 141.101.90.0 1473 typ host generation 0\r
                """;
        Map<String, Object> cloudflareSession = Map.of(
                "sessionId", "cf-app-session-1"
        );
        Map<String, Object> publishResponse = Map.of(
                "sessionDescription", Map.of("type", "answer", "sdp", cloudflareAnswerSdp),
                "tracks", List.of(Map.of("mid", "0", "trackName", "user_audio"))
        );

        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        when(callSessionRepository.findByIdAndPhoneNumber(session.getId(), existingUser.getPhoneNumber()))
                .thenReturn(Optional.of(session));
        when(cloudflareRealtimeClient.createSession(request.toCloudflarePayload())).thenReturn(cloudflareSession);
        when(cloudflareRealtimeClient.addTracks("cf-app-session-1", request.toCloudflarePayload()))
                .thenReturn(publishResponse);
        WebrtcOfferResponse response = callSessionService.submitWebrtcOffer(session.getId(), existingUser.getId(), request);

        assertThat(response.sessionDescription().get("sdp").toString()).contains("a=end-of-candidates\r\n");
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

    @Test
    void endMySessionEndsOwnedSessionWithoutTurns() {
        User user = User.createByPhone("01012345678");
        CallSession session = CallSession.create(
                user.getId(),
                user.getPhoneNumber(),
                null,
                "practice",
                "registered"
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(callSessionRepository.findByIdAndPhoneNumber(session.getId(), user.getPhoneNumber()))
                .thenReturn(Optional.of(session));

        CallSessionResponse response = callSessionService.endMySession(session.getId(), user.getId(), null);

        assertThat(response.id()).isEqualTo(session.getId());
        assertThat(response.status()).isEqualTo("completed");
        assertThat(session.getStatus()).isEqualTo("completed");
        assertThat(session.getEndedAt()).isNotNull();
    }
}
