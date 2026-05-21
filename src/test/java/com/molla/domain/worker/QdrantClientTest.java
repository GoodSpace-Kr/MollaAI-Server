package com.molla.domain.worker;

import com.molla.domain.callsession.CallSessionTurn;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QdrantClientTest {

    private final OpenAiClient openAiClient = mock(OpenAiClient.class);
    private final QdrantClient qdrantClient = new QdrantClient(
            WebClient.builder(),
            openAiClient,
            "localhost",
            6333,
            "molla_turns",
            "text-embedding-3-small"
    );

    @Test
    void buildsQdrantPointsWithUserTextVectorAndConversationPayload() {
        when(openAiClient.createEmbedding("I received the wrong item.", "text-embedding-3-small"))
                .thenReturn(List.of(0.1f, 0.2f, 0.3f));

        Map<String, Object> body = qdrantClient.buildUpsertBody(
                "user-123",
                "01012345678",
                List.of(new CallSessionTurn(
                        1,
                        OffsetDateTime.parse("2026-05-20T07:08:33.742000+00:00"),
                        new CallSessionTurn.UserTurn("I received the wrong item.", 16000, "calls/CA/test/turns/5.wav"),
                        new CallSessionTurn.AssistantTurn("I'm sorry to hear that. What item did you expect?", OffsetDateTime.parse("2026-05-20T07:08:34.646000+00:00"))
                ))
        );

        assertThat(body).containsKey("points");
        List<?> points = (List<?>) body.get("points");
        assertThat(points).hasSize(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> point = (Map<String, Object>) points.get(0);
        assertThat(point).containsKeys("id", "vector", "payload");
        assertThat(point.get("vector")).isEqualTo(List.of(0.1f, 0.2f, 0.3f));

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) point.get("payload");
        assertThat(payload).containsEntry("userId", "user-123");
        assertThat(payload).containsEntry("phoneNumber", "01012345678");
        assertThat(payload).containsEntry("userText", "I received the wrong item.");
        assertThat(payload).containsEntry("assistantText", "I'm sorry to hear that. What item did you expect?");
        assertThat((String) payload.get("createdAt")).startsWith("2026-05-20T07:08:33.742");
        assertThat(payload).containsEntry("audioKey", "calls/CA/test/turns/5.wav");
    }
}
