package com.molla.domain.worker;

import com.molla.domain.conversationturn.ConversationTurn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class QdrantClient {

    private final WebClient webClient;
    private final OpenAiClient openAiClient;
    private final String collectionName;
    private final String embeddingModel;

    public QdrantClient(
            WebClient.Builder builder,
            OpenAiClient openAiClient,
            @Value("${qdrant.host}") String host,
            @Value("${qdrant.port}") int port,
            @Value("${qdrant.collection-name}") String collectionName,
            @Value("${openai.embedding-model}") String embeddingModel
    ) {
        this.webClient = builder
                .baseUrl("http://" + host + ":" + port)
                .build();
        this.openAiClient = openAiClient;
        this.collectionName = collectionName;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 세션의 발화 텍스트를 임베딩 후 Qdrant에 upsert.
     * user 발화만 임베딩 대상 (AI 발화 제외).
     */
    public void upsertTurns(String sessionId, String userId, List<ConversationTurn> turns) {
        List<ConversationTurn> userTurns = turns.stream()
                .filter(t -> "user".equals(t.getSpeaker()))
                .toList();

        if (userTurns.isEmpty()) {
            log.info("임베딩 대상 발화 없음 — sessionId: {}", sessionId);
            return;
        }

        List<Map<String, Object>> points = userTurns.stream()
                .map(turn -> {
                    List<Float> vector = openAiClient.createEmbedding(turn.getContent(), embeddingModel);
                    return Map.<String, Object>of(
                            "id", UUID.randomUUID().toString(),
                            "vector", vector,
                            "payload", Map.of(
                                    "sessionId", sessionId,
                                    "userId", userId,
                                    "turnId", turn.getId(),
                                    "content", turn.getContent(),
                                    "sequenceOrder", turn.getSequenceOrder()
                            )
                    );
                })
                .toList();

        Map<String, Object> body = Map.of("points", points);

        webClient.put()
                .uri("/collections/" + collectionName + "/points")
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();

        log.info("Qdrant upsert 완료 — sessionId: {}, 임베딩 수: {}", sessionId, points.size());
    }
}
