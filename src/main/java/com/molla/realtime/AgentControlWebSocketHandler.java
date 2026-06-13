package com.molla.realtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class AgentControlWebSocketHandler extends TextWebSocketHandler {

    private static final TypeReference<Map<String, Object>> MESSAGE_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final AgentConnectionRegistry agentConnectionRegistry;
    private final AgentControlMessageService messageService;
    private final String agentToken;

    public AgentControlWebSocketHandler(
            ObjectMapper objectMapper,
            AgentConnectionRegistry agentConnectionRegistry,
            AgentControlMessageService messageService,
            @Value("${agents.control.token:}") String agentToken
    ) {
        this.objectMapper = objectMapper;
        this.agentConnectionRegistry = agentConnectionRegistry;
        this.messageService = messageService;
        this.agentToken = agentToken;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = resolveToken(session.getUri());
        if (!isAgentToken(token)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("valid agent token required"));
            return;
        }

        session.getAttributes().put("agentAuthenticated", true);
        agentConnectionRegistry.register(session);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "connected"
        ))));
        log.info("agent_control_connected wsSessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), MESSAGE_TYPE);
        if ("ping".equals(payload.get("type"))) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "pong"))));
            return;
        }
        try {
            messageService.handle(session, payload);
        } catch (Exception e) {
            log.error(
                    "agent_control_message_failed wsSessionId={} type={} callId={} error={}",
                    session.getId(),
                    payload.get("type"),
                    payload.get("callId"),
                    e.getMessage(),
                    e
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "agent_control_error",
                    "callId", String.valueOf(payload.getOrDefault("callId", "")),
                    "message", e.getMessage() == null ? "agent control message failed" : e.getMessage()
            ))));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        agentConnectionRegistry.unregister(session);
        log.info("agent_control_disconnected wsSessionId={} code={}", session.getId(), status.getCode());
    }

    private boolean isAgentToken(String token) {
        return StringUtils.hasText(token)
                && StringUtils.hasText(agentToken)
                && agentToken.equals(token);
    }

    private String resolveToken(URI uri) {
        if (uri == null) {
            return "";
        }
        String token = UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .getFirst("token");
        return decodeQueryParam(token);
    }

    private String decodeQueryParam(String value) {
        return value == null ? "" : URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
