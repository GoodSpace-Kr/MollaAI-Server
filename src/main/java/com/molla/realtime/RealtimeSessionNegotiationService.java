package com.molla.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RealtimeSessionNegotiationService {

    private final AgentConnectionRegistry agentConnectionRegistry;
    private final Map<String, CompletableFuture<String>> pendingSessionIds = new ConcurrentHashMap<>();

    @Value("${cloudflare.realtime.negotiation-timeout-ms:10000}")
    private long negotiationTimeoutMs;

    public String requestRealtimeSession(JoinCallCommand command) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingSessionIds.put(command.callId(), future);
        try {
            agentConnectionRegistry.sendJoinCall(command);
            return future.get(negotiationTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Realtime session negotiation failed.", e);
        } finally {
            pendingSessionIds.remove(command.callId());
        }
    }

    public void complete(String callId, String realtimeSessionId) {
        if (!StringUtils.hasText(callId) || !StringUtils.hasText(realtimeSessionId)) {
            return;
        }
        CompletableFuture<String> future = pendingSessionIds.get(callId);
        if (future != null) {
            future.complete(realtimeSessionId);
        }
    }
}
