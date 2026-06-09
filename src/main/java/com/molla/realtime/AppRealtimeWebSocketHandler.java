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
public class AppRealtimeWebSocketHandler extends TextWebSocketHandler {

    private static final TypeReference<Map<String, Object>> MESSAGE_TYPE = new TypeReference<>() {};

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = resolveToken(session.getUri());
        if (!isAccessToken(token)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("valid access token required"));
            return;
        }

        String userId = jwtProvider.getUserId(token);
        session.getAttributes().put("userId", userId);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "connected",
                "userId", userId
        ))));
        log.info("app_realtime_connected userId={} sessionId={}", userId, session.getId());
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
        log.info("app_realtime_disconnected userId={} sessionId={} code={}", userId, session.getId(), status.getCode());
    }

    private boolean isAccessToken(String token) {
        return StringUtils.hasText(token)
                && jwtProvider.validateToken(token)
                && "access".equals(jwtProvider.getTokenType(token));
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
