package com.molla.domain.worker;

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

    /**
     * transcript를 파싱해서 user 발화만 임베딩 후 Qdrant에 upsert.
     * transcript 포맷에 화자 라벨이 명확히 없으면 적재를 스킵한다.
     */
    public void upsertTranscript(String sessionId, String userId, String transcript) {
        List<TranscriptChunk> userTurns = extractUserTurns(transcript);

        if (userTurns.isEmpty()) {
            log.info("임베딩 대상 발화 없음 — sessionId: {}", sessionId);
            return;
        }

        List<Map<String, Object>> points = userTurns.stream()
                .map(turn -> {
                    List<Float> vector = openAiClient.createEmbedding(turn.content(), embeddingModel);
                    return Map.<String, Object>of(
                            "id", UUID.randomUUID().toString(),
                            "vector", vector,
                            "payload", Map.of(
                                    "sessionId", sessionId,
                                    "userId", userId,
                                    "content", turn.content(),
                                    "sequenceOrder", turn.sequenceOrder()
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

    private List<TranscriptChunk> extractUserTurns(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            return List.of();
        }

        String[] lines = transcript.split("\\R");
        List<TranscriptChunk> chunks = new ArrayList<>();
        String currentSpeaker = null;
        StringBuilder currentContent = new StringBuilder();
        int order = 0;
        boolean foundSpeakerMarkers = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            ParsedSpeakerLine parsedLine = parseSpeakerLine(line);
            if (parsedLine != null) {
                foundSpeakerMarkers = true;
                flushUserChunk(chunks, currentSpeaker, currentContent, order);

                currentSpeaker = parsedLine.speaker();
                currentContent = new StringBuilder(parsedLine.content());
                order++;
            } else if (currentContent.length() > 0) {
                currentContent.append('\n').append(line);
            } else {
                log.warn("Qdrant transcript 파싱 실패 — 화자 라벨 없는 라인 감지, line: {}", line);
                return List.of();
            }
        }

        flushUserChunk(chunks, currentSpeaker, currentContent, order);

        if (!foundSpeakerMarkers) {
            log.warn("Qdrant transcript 파싱 실패 — 화자 라벨 없음");
            return List.of();
        }

        return chunks;
    }

    private void flushUserChunk(List<TranscriptChunk> chunks, String currentSpeaker, StringBuilder currentContent, int order) {
        if (!"user".equals(currentSpeaker)) {
            return;
        }

        String content = currentContent.toString().trim();
        if (!content.isEmpty()) {
            chunks.add(new TranscriptChunk(order, content));
        }
    }

    private ParsedSpeakerLine parseSpeakerLine(String line) {
        String normalized = line.trim();
        String[] prefixes = {
                "[USER]", "USER:", "USER：", "USER-", "사용자:", "사용자：", "유저:", "유저：",
                "[AI]", "AI:", "AI：", "AI-", "ASSISTANT:", "ASSISTANT：", "BOT:", "BOT：", "SYSTEM:", "SYSTEM："
        };

        for (String prefix : prefixes) {
            if (normalized.regionMatches(true, 0, prefix, 0, prefix.length())) {
                String speaker = isUserPrefix(prefix) ? "user" : "ai";
                String content = normalized.substring(prefix.length()).trim();
                return new ParsedSpeakerLine(speaker, content);
            }
        }

        return null;
    }

    private boolean isUserPrefix(String prefix) {
        return prefix.startsWith("[USER]") || prefix.startsWith("USER") || prefix.startsWith("사용자") || prefix.startsWith("유저");
    }

    private record TranscriptChunk(
            int sequenceOrder,
            String content
    ) {
    }

    private record ParsedSpeakerLine(
            String speaker,
            String content
    ) {
    }
}
