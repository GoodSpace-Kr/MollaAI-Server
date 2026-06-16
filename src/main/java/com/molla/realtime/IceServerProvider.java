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

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class IceServerProvider {

    private final WebClient webClient;
    private final String turnTokenId;
    private final String turnApiToken;
    private final long credentialTtlSeconds;

    public IceServerProvider(
            WebClient.Builder webClientBuilder,
            @Value("${cloudflare.realtime.api-base}") String apiBase,
            @Value("${cloudflare.realtime.turn-token-id}") String turnTokenId,
            @Value("${cloudflare.realtime.turn-api-token}") String turnApiToken,
            @Value("${cloudflare.realtime.turn-credential-ttl-seconds}") long credentialTtlSeconds
    ) {
        this.webClient = webClientBuilder.baseUrl(apiBase).build();
        this.turnTokenId = turnTokenId;
        this.turnApiToken = turnApiToken;
        this.credentialTtlSeconds = credentialTtlSeconds;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getIceServers() {
        if (!StringUtils.hasText(turnTokenId) || !StringUtils.hasText(turnApiToken)) {
            return List.of(Map.of("urls", List.of("stun:stun.cloudflare.com:3478")));
        }

        String path = "/turn/keys/" + turnTokenId + "/credentials/generate-ice-servers";
        try {
            Map<?, ?> response = webClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + turnApiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("ttl", credentialTtlSeconds))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Object iceServers = response != null ? response.get("iceServers") : null;
            if (iceServers instanceof List<?> list) {
                return (List<Map<String, Object>>) list;
            }
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (WebClientResponseException e) {
            log.error("cloudflare_turn_credentials_request_failed path={} status={} response={}", path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("cloudflare_turn_credentials_request_failed path={} error={}", path, e.getMessage());
            throw new GlobalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
