package com.molla.realtime;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentControlMessageServiceTest {

    private final CloudflareRealtimeClient cloudflareRealtimeClient = mock(CloudflareRealtimeClient.class);
    private final AgentConnectionRegistry agentConnectionRegistry = mock(AgentConnectionRegistry.class);
    private final RealtimeSessionNegotiationService realtimeSessionNegotiationService = mock(RealtimeSessionNegotiationService.class);
    private final AgentControlMessageService service = new AgentControlMessageService(
            cloudflareRealtimeClient,
            agentConnectionRegistry,
            realtimeSessionNegotiationService
    );

    @Test
    void agentWebrtcOfferCreatesCloudflareSessionAndAnswerIsSentBackToAgent() {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> offer = Map.of(
                "type", "agent_webrtc_offer",
                "callId", "call-1",
                "realtimeSessionId", "",
                "sessionDescription", Map.of("type", "offer", "sdp", "local-sdp")
        );
        Map<String, Object> answer = Map.of("type", "answer", "sdp", "remote-sdp");
        when(cloudflareRealtimeClient.createSession(offer))
                .thenReturn(Map.of(
                        "sessionId", "cf-session-1",
                        "sessionDescription", answer
                ));

        service.handle(session, offer);

        verify(cloudflareRealtimeClient).createSession(offer);
        verify(realtimeSessionNegotiationService).complete("call-1", "cf-session-1");
        verify(agentConnectionRegistry).send(
                session,
                Map.of(
                        "type", "webrtc_answer",
                        "callId", "call-1",
                        "realtimeSessionId", "cf-session-1",
                        "sessionDescription", answer
                )
        );
    }

    @Test
    void agentRenegotiationOfferIsForwardedToCloudflareAndAnswerIsSentBackToAgent() {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> offer = Map.of(
                "type", "agent_webrtc_renegotiation_offer",
                "callId", "call-1",
                "realtimeSessionId", "cf-session-1",
                "sessionDescription", Map.of("type", "offer", "sdp", "renegotiation-sdp")
        );
        Map<String, Object> answer = Map.of("type", "answer", "sdp", "remote-sdp");
        when(cloudflareRealtimeClient.renegotiateSession("cf-session-1", offer))
                .thenReturn(Map.of("sessionDescription", answer));

        service.handle(session, offer);

        verify(cloudflareRealtimeClient).renegotiateSession("cf-session-1", offer);
        verify(agentConnectionRegistry).send(
                session,
                Map.of(
                        "type", "webrtc_answer",
                        "callId", "call-1",
                        "realtimeSessionId", "cf-session-1",
                        "sessionDescription", answer
                )
        );
    }
}
