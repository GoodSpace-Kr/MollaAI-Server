package com.molla.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentControlMessageService {

    private final CloudflareRealtimeClient cloudflareRealtimeClient;
    private final AgentConnectionRegistry agentConnectionRegistry;
    private final RealtimeSessionNegotiationService realtimeSessionNegotiationService;

    public void handle(WebSocketSession session, Map<String, Object> payload) {
        String type = String.valueOf(payload.getOrDefault("type", ""));
        if ("agent_webrtc_offer".equals(type)) {
            handleAgentWebrtcOffer(session, payload);
        }
    }

    private void handleAgentWebrtcOffer(WebSocketSession session, Map<String, Object> payload) {
        String callId = String.valueOf(payload.getOrDefault("callId", ""));
        Map<String, Object> response = cloudflareRealtimeClient.createSession(payload);
        String realtimeSessionId = String.valueOf(response.getOrDefault("sessionId", ""));
        Object sessionDescription = response.get("sessionDescription");
        realtimeSessionNegotiationService.complete(callId, realtimeSessionId);
        agentConnectionRegistry.send(
                session,
                Map.of(
                        "type", "webrtc_answer",
                        "callId", callId,
                        "realtimeSessionId", realtimeSessionId,
                        "sessionDescription", sessionDescription
                )
        );
    }
}
