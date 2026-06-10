package com.molla.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class AgentConnectionRegistry {

    private final ObjectMapper objectMapper;
    private final AtomicReference<WebSocketSession> connectedAgent = new AtomicReference<>();

    public void register(WebSocketSession session) {
        WebSocketSession previous = connectedAgent.getAndSet(session);
        if (previous != null && previous.isOpen() && !previous.getId().equals(session.getId())) {
            try {
                previous.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void unregister(WebSocketSession session) {
        connectedAgent.compareAndSet(session, null);
    }

    public Optional<WebSocketSession> current() {
        WebSocketSession session = connectedAgent.get();
        if (session == null || !session.isOpen()) {
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void sendJoinCall(JoinCallCommand command) {
        WebSocketSession session = current()
                .orElseThrow(() -> new IllegalStateException("No connected AI orchestrator agent."));
        send(session, command);
    }

    public void send(WebSocketSession session, Object payload) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send agent control message.", e);
        }
    }
}
