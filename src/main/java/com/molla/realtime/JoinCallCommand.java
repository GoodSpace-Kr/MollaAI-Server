package com.molla.realtime;

import java.util.Map;

public record JoinCallCommand(
        String type,
        String callId,
        String sessionId,
        String userId,
        Realtime realtime
) {
    public static JoinCallCommand of(String callId, String sessionId, String userId, String realtimeSessionId) {
        return new JoinCallCommand(
                "join_call",
                callId,
                sessionId,
                userId,
                new Realtime(
                        realtimeSessionId,
                        Map.of(
                                "subscribe", "user_audio",
                                "publish", "assistant_audio"
                        )
                )
        );
    }

    public record Realtime(
            String sessionId,
            Map<String, String> tracks
    ) {
    }
}
