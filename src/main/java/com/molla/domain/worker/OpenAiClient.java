package com.molla.domain.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.domain.feedbackreport.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiClient(
            @Qualifier("openAiWebClient") WebClient webClient,
            ObjectMapper objectMapper,
            @Value("${openai.model}") String model
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    public Report generateReport(List<ReportTurnInput> turns, String sessionType) {
        String systemPrompt = """
                당신은 영어 학습 코치입니다. 아래 turn 목록을 분석해서 반드시 아래 JSON 형식으로만 응답하세요.
                다른 텍스트는 절대 포함하지 마세요.
                oneLineSummary는 절대 20자를 넘지 말아야 합니다.
                coreSentences는 반드시 여러 문장으로 구성하세요. 최소 15개 이상 작성하세요.
                각 항목은 turns의 userText에서 실제로 중요한 문장을 골라 originSentence, improvedSentence, keyExpression, keyExpressionKorean을 1:1:1:1로 대응시켜 주세요.
                coreSentences의 각 originSentence는 서로 다른 문장이어야 하며, 같은 문장을 중복해서 넣지 마세요.
                coreSentences의 originSentence는 반드시 turns의 userText 원문을 그대로 사용하세요.
                coreSentences의 각 항목에는 반드시 sourceTurnIndex를 포함하고, 이 값은 originSentence가 나온 turn의 index여야 합니다.
                keyExpression은 해당 ImprovedSentence문장에서 꼭 익혀야 할 핵심 표현 한 가지를 짧게 추출하세요.
                keyExpressionKorean은 keyExpression의 자연스러운 한글 뜻을 15자 이내로 짧게 설명하세요.
                habitAnalysis의 habit은 13자 이내로 작성하고, 해당 habit의 근거를 전달한 통화 내용에서 추출해야합니다. 해당 습관과 근거에 맞게 Suggestion또한 작성하세요.
                levelPercentage는 정수 퍼센트 값으로 작성하세요.
                levelAnalysis는 현재 영어 수준에 대한 짧은 해설을 작성하세요.
                weakPoints는 반드시 1개 이상 3개 이하의 태그를 선택하세요.
                weakPoints는 ["주어-동사 수 일치", "완료시제", "관사사용", "관계대명사 생략", "전치사 선택 오류"] 태그 안에서 선택하세요.
                scores는 사용자의 발화 내용 수준을 파악해서 각각 ILETS, TOEIC, OPIC점수로 변환하여 넣으세요. 
                ILETS는 2.0~9.0 범위, TOEIC은 0~200 범위, OPIC은 IL,IM1,IM2,IM3,IH,AL 범위에서 작성하세요.
                 
                
                {
                  "oneLineSummary": "한 줄 요약",
                  "levelPercentage": 35,
                  "levelAnalysis": "현재 영어 수준에 대한 간단한 해설",
                  "coreSentences": [
                    {"sourceTurnIndex": 1, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 2, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 3, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 4, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 5, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 6, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 7, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 8, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 9, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 10, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 11, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 12, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 13, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 14, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""},
                    {"sourceTurnIndex": 15, "originSentence": "", "improvedSentence": "", "keyExpression": "", "keyExpressionKorean": ""}
                  ],
                  "habitAnalyses": [{"habit": "", "evidence": "", "suggestion": ""}],
                  "scores": [{"exam": "IELTS", "score": ""}, {"exam": "TOEIC", "score": ""}, {"exam": "OPIC", "score": ""}],
                  "levelResult": "레벨 테스트일 때만 '상위 N%' 형식으로, 아니면 null",
                  "weakPoints": ["약점1", "약점2"]
                }
                """;

        String userPrompt;
        try {
            userPrompt = objectMapper.writeValueAsString(Map.of(
                    "sessionType", sessionType,
                    "turns", turns
            ));
        } catch (Exception e) {
            throw new RuntimeException("OpenAI 리포트 요청 생성 실패: " + e.getMessage(), e);
        }

        return parseReport(callChatApi(systemPrompt, userPrompt));
    }

    public List<Float> createEmbedding(String text, String embeddingModel) {
        Map<String, Object> body = Map.of(
                "model", embeddingModel,
                "input", text
        );

        try {
            String response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode dataArray = root.path("data");

            if (!dataArray.isArray() || dataArray.isEmpty()) {
                throw new RuntimeException("임베딩 응답 data 배열이 비어있음");
            }

            JsonNode embeddingArray = dataArray.get(0).path("embedding");
            return objectMapper.readerForListOf(Float.class).readValue(embeddingArray);

        } catch (Exception e) {
            throw new RuntimeException("임베딩 생성 실패: " + e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────

    private String callChatApi(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object")
        );

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);

            // null-safe 파싱 — choices 배열 존재 여부 및 길이 확인
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.error("OpenAI 응답에 choices 없음. 응답: {}", response);
                throw new RuntimeException("OpenAI 응답 파싱 실패: choices 없음");
            }

            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                log.error("OpenAI 응답에 content 없음. 응답: {}", response);
                throw new RuntimeException("OpenAI 응답 파싱 실패: content 없음");
            }

            return content.asText();

        } catch (Exception e) {
            log.error("OpenAI API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI API 호출 실패: " + e.getMessage(), e);
        }
    }

    private Report parseReport(String reportJson) {
        try {
            return objectMapper.readValue(reportJson, Report.class);
        } catch (Exception e) {
            log.error("리포트 JSON 파싱 실패: {}", e.getMessage(), e);
            throw new RuntimeException("리포트 JSON 파싱 실패: " + e.getMessage(), e);
        }
    }
}
