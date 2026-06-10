package com.molla.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("worker websocket connected sessionId={} remote={}", session.getId(), session.getRemoteAddress());
        session.sendMessage(new TextMessage(connectedMessage()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("worker websocket received sessionId={} payload={}", session.getId(), message.getPayload());
        session.sendMessage(new TextMessage(echoMessage(message.getPayload())));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info(
                "worker websocket disconnected sessionId={} code={} reason={}",
                session.getId(),
                status.getCode(),
                status.getReason()
        );
    }

    String connectedMessage() throws JsonProcessingException {
        return objectMapper.writeValueAsString(Map.of(
                "type", "connected",
                "message", "worker ws ready"
        ));
    }

    String echoMessage(String payload) throws IOException {
        Object received;
        try {
            received = objectMapper.readValue(payload, Object.class);
        } catch (JsonProcessingException ex) {
            received = payload;
        }

        return objectMapper.writeValueAsString(Map.of(
                "type", "echo",
                "received", received
        ));
    }
}
