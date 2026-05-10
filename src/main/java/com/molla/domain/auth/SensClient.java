package com.molla.domain.auth;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SensClient {

    private final WebClient webClient;
    private final String accessKey;
    private final String secretKey;
    private final String serviceId;
    private final String fromNumber;

    public SensClient(
            WebClient.Builder webClientBuilder,
            @Value("${naver.sens.base-url}") String baseUrl,
            @Value("${naver.sens.access-key}") String accessKey,
            @Value("${naver.sens.secret-key}") String secretKey,
            @Value("${naver.sens.service-id}") String serviceId,
            @Value("${naver.sens.from-number}") String fromNumber
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.serviceId = serviceId;
        this.fromNumber = fromNumber;
    }

    public void sendSms(String toNumber, String content) {
        String url = "/sms/v2/services/" + serviceId + "/messages";
        long timestamp = System.currentTimeMillis();
        String signature = makeSignature(timestamp, url);

        Map<String, Object> body = Map.of(
                "type", "SMS",
                "from", fromNumber,
                "content", content,
                "messages", List.of(Map.of("to", toNumber))
        );

        try {
            webClient.post()
                    .uri(url)
                    .header("x-ncp-apigw-timestamp", String.valueOf(timestamp))
                    .header("x-ncp-iam-access-key", accessKey)
                    .header("x-ncp-apigw-signature-v2", signature)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("SMS 발송 성공 — to: {}", toNumber);
        } catch (Exception e) {
            log.error("SMS 발송 실패 — to: {}, error: {}", toNumber, e.getMessage());
            throw new GlobalException(ErrorCode.SMS_SEND_FAILED);
        }
    }

    private String makeSignature(long timestamp, String url) {
        try {
            String message = "POST " + url + "\n" + timestamp + "\n" + accessKey;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.encodeBase64String(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new GlobalException(ErrorCode.SMS_SEND_FAILED);
        }
    }
}
