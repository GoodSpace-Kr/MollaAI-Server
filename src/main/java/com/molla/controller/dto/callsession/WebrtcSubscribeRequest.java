package com.molla.controller.dto.callsession;

import jakarta.validation.constraints.NotBlank;

public record WebrtcSubscribeRequest(
        @NotBlank
        String agentRealtimeSessionId,

        @NotBlank
        String appRealtimeSessionId,

        String trackName
) {
    public String resolvedTrackName() {
        return trackName == null || trackName.isBlank() ? "user_audio" : trackName;
    }
}
