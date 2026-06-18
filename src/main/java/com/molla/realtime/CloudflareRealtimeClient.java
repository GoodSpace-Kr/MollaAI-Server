package com.molla.realtime;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@Component
public class CloudflareRealtimeClient {

    private final WebClient webClient;
    private final String appId;
    private final String apiToken;

    public CloudflareRealtimeClient(
            WebClient.Builder webClientBuilder,
            @Value("${cloudflare.realtime.api-base}") String apiBase,
            @Value("${cloudflare.realtime.app-id}") String appId,
            @Value("${cloudflare.realtime.api-token}") String apiToken
    ) {
        this.webClient = webClientBuilder.baseUrl(apiBase).build();
        this.appId = appId;
        this.apiToken = apiToken;
    }

    public Map<String, Object> createSession() {
        return createSession(Map.of());
    }

    public Map<String, Object> createSession(Map<String, Object> offerPayload) {
        ensureConfigured();
        Map<String, Object> request = new HashMap<>();
        Object sessionDescription = offerPayload.get("sessionDescription");
        if (sessionDescription != null) {
            request.put("sessionDescription", sessionDescription);
        }
        Map<String, Object> response = post("/apps/" + appId + "/sessions/new", request);
        Object sessionId = response.get("sessionId");
        if (!(sessionId instanceof String value) || !StringUtils.hasText(value)) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    public Map<String, Object> addTracks(String realtimeSessionId, Map<String, Object> offerPayload) {
        ensureConfigured();
        return post("/apps/" + appId + "/sessions/" + realtimeSessionId + "/tracks/new", offerPayload);
    }

    public Map<String, Object> getSessionState(String realtimeSessionId) {
        ensureConfigured();
        return get("/apps/" + appId + "/sessions/" + realtimeSessionId);
    }

    public Map<String, Object> renegotiateSession(String realtimeSessionId, Map<String, Object> offerPayload) {
        ensureConfigured();
        Map<String, Object> request = Map.of(
                "sessionDescription", offerPayload.get("sessionDescription")
        );
        return put("/apps/" + appId + "/sessions/" + realtimeSessionId + "/renegotiate", request);
    }

    private Map<String, Object> post(String path, Object body) {
        try {
            Map<?, ?> response = webClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            entry -> String.valueOf(entry.getKey()),
                            Map.Entry::getValue
                    ));
        } catch (WebClientResponseException e) {
            log.error("cloudflare_realtime_request_failed path={} status={} response={}", path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("cloudflare_realtime_request_failed path={} error={}", path, e.getMessage());
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> get(String path) {
        try {
            Map<?, ?> response = webClient.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            entry -> String.valueOf(entry.getKey()),
                            Map.Entry::getValue
                    ));
        } catch (WebClientResponseException e) {
            log.error("cloudflare_realtime_request_failed path={} status={} response={}", path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("cloudflare_realtime_request_failed path={} error={}", path, e.getMessage());
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> put(String path, Object body) {
        try {
            Map<?, ?> response = webClient.put()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return response.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            entry -> String.valueOf(entry.getKey()),
                            Map.Entry::getValue
                    ));
        } catch (WebClientResponseException e) {
            log.error("cloudflare_realtime_request_failed path={} status={} response={}", path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("cloudflare_realtime_request_failed path={} error={}", path, e.getMessage());
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(apiToken)) {
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
