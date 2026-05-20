package com.molla.domain.worker;

import com.molla.domain.callsession.CallSessionTurn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
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

    public void upsertTurns(String sessionId, String userId, String phoneNumber, List<CallSessionTurn> turns) {
        List<TranscriptChunk> userTurns = extractUserTurns(turns);

        if (userTurns.isEmpty()) {
            log.info("임베딩 대상 발화 없음 — sessionId: {}", sessionId);
            return;
        }

        List<Map<String, Object>> points = userTurns.stream()
                .map(turn -> {
                    List<Float> vector = openAiClient.createEmbedding(turn.content(), embeddingModel);
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("sessionId", sessionId);
                    payload.put("phoneNumber", phoneNumber);
                    payload.put("content", turn.content());
                    payload.put("sequenceOrder", turn.sequenceOrder());
                    if (userId != null) {
                        payload.put("userId", userId);
                    }

                    return Map.<String, Object>of(
                            "id", UUID.randomUUID().toString(),
                            "vector", vector,
                            "payload", payload
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

    private List<TranscriptChunk> extractUserTurns(List<CallSessionTurn> turns) {
        if (turns == null || turns.isEmpty()) {
            return List.of();
        }

        List<TranscriptChunk> chunks = new ArrayList<>();
        for (CallSessionTurn turn : turns) {
            if (turn == null || turn.user() == null || turn.user().text() == null) {
                continue;
            }

            String content = turn.user().text().trim();
            if (content.isEmpty()) {
                continue;
            }

            chunks.add(new TranscriptChunk(
                    turn.index() != null ? turn.index() : chunks.size() + 1,
                    content
            ));
        }

        return chunks;
    }

    private record TranscriptChunk(
            int sequenceOrder,
            String content
    ) {
    }
}
