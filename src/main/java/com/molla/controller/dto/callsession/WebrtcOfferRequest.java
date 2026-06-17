package com.molla.controller.dto.callsession;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record WebrtcOfferRequest(
        @NotBlank
        String agentRealtimeSessionId,

        @NotEmpty
        Map<String, Object> sessionDescription,

        List<Map<String, Object>> tracks
) {
    public Map<String, Object> toCloudflarePayload() {
        return Map.of(
                "sessionDescription", sessionDescription,
                "tracks", tracks == null ? List.of() : tracks
        );
    }
}
