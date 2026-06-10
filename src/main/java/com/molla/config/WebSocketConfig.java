package com.molla.config;

import com.molla.realtime.AgentControlWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AgentControlWebSocketHandler agentControlWebSocketHandler;
    private final WorkerWebSocketHandler workerWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(agentControlWebSocketHandler, "/api/v1/agents/control")
                .setAllowedOriginPatterns("*");

        registry.addHandler(workerWebSocketHandler, "/workers/ws")
                .setAllowedOriginPatterns("*");
    }
}
