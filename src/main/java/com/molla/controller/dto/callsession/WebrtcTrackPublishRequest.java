package com.molla.controller.dto.callsession;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record WebrtcTrackPublishRequest(
        @NotBlank
        String agentRealtimeSessionId,

        @NotBlank
        String appRealtimeSessionId,

        @NotEmpty
        Map<String, Object> sessionDescription,

        List<Map<String, Object>> tracks
) {
    public Map<String, Object> toCloudflarePayload() {
        return Map.of(
                "sessionDescription", sessionDescription,
                "tracks", tracks == null ? List.of() : normalizeLocalTracks(tracks)
        );
    }

    private List<Map<String, Object>> normalizeLocalTracks(List<Map<String, Object>> sourceTracks) {
        return sourceTracks.stream()
                .map(track -> {
                    if (!"local".equals(String.valueOf(track.get("location")))) {
                        return track;
                    }
                    Map<String, Object> normalized = new HashMap<>(track);
                    normalized.putIfAbsent("kind", "audio");
                    normalized.putIfAbsent("bidirectionalMediaStream", true);
                    return normalized;
                })
                .toList();
    }
}
