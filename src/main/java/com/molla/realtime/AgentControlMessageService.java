package com.molla.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentControlMessageService {

    private final CloudflareRealtimeClient cloudflareRealtimeClient;
    private final AgentConnectionRegistry agentConnectionRegistry;

    public void handle(WebSocketSession session, Map<String, Object> payload) {
        String type = String.valueOf(payload.getOrDefault("type", ""));
        if ("agent_webrtc_offer".equals(type)) {
            handleAgentWebrtcOffer(session, payload);
        }
    }

    private void handleAgentWebrtcOffer(WebSocketSession session, Map<String, Object> payload) {
        String callId = String.valueOf(payload.getOrDefault("callId", ""));
        String realtimeSessionId = String.valueOf(payload.getOrDefault("realtimeSessionId", ""));
        Map<String, Object> response;
        if (StringUtils.hasText(realtimeSessionId)) {
            response = cloudflareRealtimeClient.addTracks(realtimeSessionId, payload);
        } else {
            response = cloudflareRealtimeClient.createSession(payload);
            realtimeSessionId = String.valueOf(response.getOrDefault("sessionId", ""));
        }
        Object sessionDescription = response.get("sessionDescription");
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
