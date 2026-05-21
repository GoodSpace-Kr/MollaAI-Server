package com.molla.domain.worker;

import com.molla.domain.callsession.CallSessionTurn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
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

        Map<String, Object> body = buildUpsertBody(userId, phoneNumber, turns);

        // AI 서버의 FastAPI 엔드포인트 연결 시 아래 호출을 그쪽 계약에 맞게 되살리면 됩니다.
        /*
        webClient.put()
                .uri("/collections/" + collectionName + "/points")
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
        */

        log.info("Qdrant upsert body 준비 완료 — sessionId: {}, 임베딩 수: {}", sessionId, userTurns.size());
    }

    Map<String, Object> buildUpsertBody(String userId, String phoneNumber, List<CallSessionTurn> turns) {
        List<TranscriptChunk> userTurns = extractUserTurns(turns);

        List<Map<String, Object>> points = userTurns.stream()
                .map(turn -> {
                    List<Float> vector = openAiClient.createEmbedding(turn.userText(), embeddingModel);
                    Map<String, Object> payload = new java.util.LinkedHashMap<>();
                    payload.put("userId", userId);
                    payload.put("phoneNumber", phoneNumber);
                    payload.put("userText", turn.userText());
                    payload.put("assistantText", turn.assistantText());
                    payload.put("createdAt", turn.createdAt());
                    payload.put("audioKey", turn.audioKey());

                    return Map.<String, Object>of(
                            "id", UUID.randomUUID().toString(),
                            "vector", vector,
                            "payload", payload
                    );
                })
                .toList();

        return Map.of("points", points);
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
                    content,
                    turn.assistant() != null ? trimToNull(turn.assistant().text()) : null,
                    turn.createdAt() != null ? turn.createdAt().toString() : null,
                    turn.user().audioKey()
            ));
        }

        return chunks;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record TranscriptChunk(
            String userText,
            String assistantText,
            String createdAt,
            String audioKey
    ) {
    }
}
