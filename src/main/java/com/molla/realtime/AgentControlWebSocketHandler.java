package com.molla.realtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.config.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentControlWebSocketHandler extends TextWebSocketHandler {

    private static final TypeReference<Map<String, Object>> MESSAGE_TYPE = new TypeReference<>() {};

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = resolveToken(session.getUri());
        if (!isAgentToken(token)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("valid agent token required"));
            return;
        }

        String userId = jwtProvider.getUserId(token);
        String callSessionId = jwtProvider.getSessionId(token);
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("callSessionId", callSessionId);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "connected",
                "userId", userId,
                "callSessionId", callSessionId
        ))));
        log.info("agent_control_connected userId={} callSessionId={} wsSessionId={}", userId, callSessionId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), MESSAGE_TYPE);
        if ("ping".equals(payload.get("type"))) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "pong"))));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get("userId");
        Object callSessionId = session.getAttributes().get("callSessionId");
        log.info("agent_control_disconnected userId={} callSessionId={} wsSessionId={} code={}",
                userId, callSessionId, session.getId(), status.getCode());
    }

    private boolean isAgentToken(String token) {
        return StringUtils.hasText(token)
                && jwtProvider.validateToken(token)
                && "agent".equals(jwtProvider.getTokenType(token))
                && "agent:control".equals(jwtProvider.getScope(token));
    }

    private String resolveToken(URI uri) {
        if (uri == null) {
            return "";
        }
        return UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .getFirst("token");
    }
}
