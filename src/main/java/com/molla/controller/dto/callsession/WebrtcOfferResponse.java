package com.molla.controller.dto.callsession;

import java.util.List;
import java.util.Map;

public record WebrtcOfferResponse(
        Map<String, Object> sessionDescription,
        List<Map<String, Object>> tracks
) {
    @SuppressWarnings("unchecked")
    public static WebrtcOfferResponse fromCloudflare(Map<String, Object> payload) {
        Object description = payload.get("sessionDescription");
        Object tracks = payload.get("tracks");
        return new WebrtcOfferResponse(
                description instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of(),
                tracks instanceof List<?> list ? (List<Map<String, Object>>) list : List.of()
        );
    }
}
