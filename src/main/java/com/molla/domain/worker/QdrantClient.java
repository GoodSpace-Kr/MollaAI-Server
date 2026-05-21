package com.molla.domain.worker;

import com.molla.domain.callsession.CallSessionTurn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class QdrantClient {

    private static final String MEMORY_POINTS_URI = "/memory/points";

    private final WebClient webClient;
    private final OpenAiClient openAiClient;
    private final String embeddingModel;

    public QdrantClient(
            WebClient.Builder builder,
            OpenAiClient openAiClient,
            @org.springframework.beans.factory.annotation.Value("${openai.embedding-model}") String embeddingModel
    ) {
        this.webClient = builder
                .baseUrl("https://orch.mollatalk.com")
                .build();
        this.openAiClient = openAiClient;
        this.embeddingModel = embeddingModel;
    }

    public void upsertTurns(String sessionId, String userId, String phoneNumber, List<CallSessionTurn> turns) {
        List<TranscriptChunk> userTurns = extractUserTurns(turns);
        if (userTurns.isEmpty()) {
            log.info("임베딩 대상 발화 없음 — sessionId: {}", sessionId);
            return;
        }

        Map<String, Object> body = buildUpsertBody(userId, phoneNumber, turns);

        try {
            webClient.post()
                    .uri(MEMORY_POINTS_URI)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "memory points upload failed status=%s body=%s".formatted(
                            e.getStatusCode(),
                            e.getResponseBodyAsString()
                    ),
                    e
            );
        }

        log.info("메모리 포인트 업로드 완료 — sessionId: {}, 임베딩 수: {}", sessionId, userTurns.size());
    }

    Map<String, Object> buildUpsertBody(String userId, String phoneNumber, List<CallSessionTurn> turns) {
        List<TranscriptChunk> userTurns = extractUserTurns(turns);

        List<Map<String, Object>> points = userTurns.stream()
                .map(turn -> {
                    List<Float> vector = openAiClient.createEmbedding(turn.userText(), embeddingModel);
                    Map<String, Object> payload = new LinkedHashMap<>();
                    putIfNotBlank(payload, "userId", userId);
                    putIfNotBlank(payload, "phoneNumber", phoneNumber);
                    putIfNotBlank(payload, "userText", turn.userText());
                    putIfNotBlank(payload, "assistantText", turn.assistantText());
                    putIfNotBlank(payload, "createdAt", turn.createdAt());
                    putIfNotBlank(payload, "audioKey", turn.audioKey());

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

    private void putIfNotBlank(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }

    private record TranscriptChunk(
            String userText,
            String assistantText,
            String createdAt,
            String audioKey
    ) {
    }
}
