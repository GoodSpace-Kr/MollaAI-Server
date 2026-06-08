package com.molla.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerWebSocketHandlerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WorkerWebSocketHandler handler = new WorkerWebSocketHandler(objectMapper);

    @Test
    void connectedMessageMatchesConnectivityContract() throws Exception {
        JsonNode message = objectMapper.readTree(handler.connectedMessage());

        assertThat(message.get("type").asText()).isEqualTo("connected");
        assertThat(message.get("message").asText()).isEqualTo("worker ws ready");
    }

    @Test
    void echoMessageWrapsReceivedJsonPayload() throws Exception {
        String payload = """
                {
                  "type": "heartbeat",
                  "workerId": "campus-2080ti-test",
                  "seq": 2
                }
                """;

        JsonNode message = objectMapper.readTree(handler.echoMessage(payload));

        assertThat(message.get("type").asText()).isEqualTo("echo");
        assertThat(message.get("received").get("type").asText()).isEqualTo("heartbeat");
        assertThat(message.get("received").get("workerId").asText()).isEqualTo("campus-2080ti-test");
        assertThat(message.get("received").get("seq").asInt()).isEqualTo(2);
    }
}
