package com.molla.domain.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class NicepayClient {

    // ──────────────────────────────────────────────
    // [환경변수 교체 필요]
    // 테스트: application.yml의 nicepay.client-key, nicepay.secret-key
    // 실서비스 전환 시 해당 값을 실서비스 키로 교체하세요.
    // ──────────────────────────────────────────────

    private final WebClient webClient;
    private final String clientKey;
    private final String secretKey;

    public NicepayClient(
            WebClient.Builder webClientBuilder,
            // [교체 필요] 테스트 → 실서비스 전환 시 application.yml의 nicepay.base-url 변경
            @Value("${nicepay.base-url:https://sandbox-api.nicepay.co.kr}") String baseUrl,
            @Value("${nicepay.client-key}") String clientKey,
            @Value("${nicepay.secret-key}") String secretKey
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.clientKey = clientKey;
        this.secretKey = secretKey;
    }

    /**
     * 나이스페이 결제 승인 API 호출.
     *
     * @param tid    나이스페이 인증 후 발급된 거래 ID
     * @param amount 결제 금액 (위변조 검증용)
     * @return 승인 응답 (resultCode, tid, orderId 등)
     */
    public Map<String, Object> approve(String tid, int amount) {
        String authorization = makeBasicAuth();

        Map<String, Object> body = Map.of("amount", amount);

        try {
            // [교체 필요] 테스트 URL: https://sandbox-api.nicepay.co.kr/v1/payments/{tid}
            // 실서비스 URL: https://api.nicepay.co.kr/v1/payments/{tid}
            // base-url을 application.yml에서 교체하면 아래 코드는 변경 불필요
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/v1/payments/{tid}", tid)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("나이스페이 승인 응답 — tid: {}, resultCode: {}",
                    tid, response != null ? response.get("resultCode") : "null");

            return response;

        } catch (Exception e) {
            log.error("나이스페이 승인 API 호출 실패 — tid: {}, error: {}", tid, e.getMessage(), e);
            throw new PaymentException("나이스페이 승인 API 호출 실패: " + e.getMessage());
        }
    }

    private String makeBasicAuth() {
        // Basic 인증: Base64(clientKey:secretKey)
        String credentials = clientKey + ":" + secretKey;
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
