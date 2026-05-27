package com.molla.domain.auth;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class SensClientTest {

    @Test
    void buildsRawServicePathForRequestAndSignature() {
        SensClient sensClient = new SensClient(
                WebClient.builder(),
                "https://sens.apigw.ntruss.com",
                "access-key",
                "secret-key",
                "ncp:sms:kr:370221810170:molla-sms",
                "01057807344"
        );

        String path = sensClient.buildMessagePath();

        assertThat(path)
                .isEqualTo("/sms/v2/services/ncp:sms:kr:370221810170:molla-sms/messages");
    }
}
