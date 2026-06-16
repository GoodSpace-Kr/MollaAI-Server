package com.molla.controller.dto.callsession;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public record WebrtcOfferResponse(
        String realtimeSessionId,
        Map<String, Object> sessionDescription,
        List<Map<String, Object>> tracks
) {
    @SuppressWarnings("unchecked")
    public static WebrtcOfferResponse fromCloudflare(Map<String, Object> payload) {
        Object description = payload.get("sessionDescription");
        Object tracks = payload.get("tracks");
        Object sessionId = payload.get("sessionId");
        return new WebrtcOfferResponse(
                sessionId instanceof String value ? value : null,
                description instanceof Map<?, ?> map ? withEndOfCandidates((Map<String, Object>) map) : Map.of(),
                tracks instanceof List<?> list ? (List<Map<String, Object>>) list : List.of()
        );
    }

    private static Map<String, Object> withEndOfCandidates(Map<String, Object> description) {
        Object sdpValue = description.get("sdp");
        if (!(sdpValue instanceof String sdp)
                || !sdp.contains("a=candidate:")
                || sdp.contains("a=end-of-candidates")) {
            return description;
        }

        Map<String, Object> normalized = new HashMap<>(description);
        normalized.put("sdp", appendSdpLine(sdp, "a=end-of-candidates"));
        return normalized;
    }

    private static String appendSdpLine(String sdp, String line) {
        String lineEnding = sdp.contains("\r\n") ? "\r\n" : "\n";
        String normalizedSdp = sdp.endsWith("\r\n") || sdp.endsWith("\n") ? sdp : sdp + lineEnding;
        return normalizedSdp + line + lineEnding;
    }
}
