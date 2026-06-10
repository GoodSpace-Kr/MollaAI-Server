package com.molla.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final AgentConnectionRegistry agentConnectionRegistry = mock(AgentConnectionRegistry.class);
    private final AgentControlMessageService messageService = mock(AgentControlMessageService.class);
    private final AgentControlWebSocketHandler handler = new AgentControlWebSocketHandler(
            new ObjectMapper(),
            agentConnectionRegistry,
            messageService,
            "agent-token"
    );

    @Test
    void acceptsConfiguredAgentTokenAndRegistersConnectedAgent() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        WebSocketSession session = mock(WebSocketSession.class);

        when(session.getUri()).thenReturn(URI.create("wss://api.example.com/api/v1/agents/control?token=agent-token"));
        when(session.getAttributes()).thenReturn(attributes);

        handler.afterConnectionEstablished(session);

        assertThat(attributes).containsEntry("agentAuthenticated", true);
        verify(agentConnectionRegistry).register(session);
        verify(session).sendMessage(any(TextMessage.class));
        verify(session, never()).close(any(CloseStatus.class));
    }

    @Test
    void closesConnectionWhenTokenIsInvalid() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);

        when(session.getUri()).thenReturn(URI.create("wss://api.example.com/api/v1/agents/control?token=bad-token"));

        handler.afterConnectionEstablished(session);

        verify(session).close(any(CloseStatus.class));
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void delegatesNonPingMessagesToAgentControlMessageService() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-1");

        handler.handleTextMessage(
                session,
                new TextMessage("{\"type\":\"agent_webrtc_offer\",\"callId\":\"call-1\"}")
        );

        verify(messageService).handle(session, Map.of("type", "agent_webrtc_offer", "callId", "call-1"));
    }
}
