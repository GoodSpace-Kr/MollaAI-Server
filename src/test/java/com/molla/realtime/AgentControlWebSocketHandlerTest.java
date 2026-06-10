package com.molla.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.config.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentControlWebSocketHandlerTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final AgentControlWebSocketHandler handler = new AgentControlWebSocketHandler(
            jwtProvider,
            new ObjectMapper()
    );

    @Test
    void acceptsAgentTokenFromQueryAndSendsConnectedEvent() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        WebSocketSession session = mock(WebSocketSession.class);

        when(session.getUri()).thenReturn(URI.create("wss://api.example.com/api/v1/agents/control?token=agent-token"));
        when(session.getAttributes()).thenReturn(attributes);
        when(jwtProvider.validateToken("agent-token")).thenReturn(true);
        when(jwtProvider.getTokenType("agent-token")).thenReturn("agent");
        when(jwtProvider.getScope("agent-token")).thenReturn("agent:control");
        when(jwtProvider.getUserId("agent-token")).thenReturn("user-1");
        when(jwtProvider.getSessionId("agent-token")).thenReturn("session-1");

        handler.afterConnectionEstablished(session);

        assertThat(attributes).containsEntry("userId", "user-1");
        assertThat(attributes).containsEntry("callSessionId", "session-1");
        verify(session).sendMessage(any(TextMessage.class));
        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void closesConnectionWhenTokenIsInvalid() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);

        when(session.getUri()).thenReturn(URI.create("wss://api.example.com/api/v1/agents/control?token=bad-token"));
        when(jwtProvider.validateToken("bad-token")).thenReturn(false);

        handler.afterConnectionEstablished(session);

        verify(session).close(any(CloseStatus.class));
        verify(session, never()).sendMessage(any(TextMessage.class));
    }
}
