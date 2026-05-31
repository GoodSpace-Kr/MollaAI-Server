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
                IELTS는 실제 시험 응시자 평균(5.5~6.0)을 기준으로 상대적으로 평가하되, 기초 수준도 3.0 이상을 유지하세요.
                점수는 실제 학습자를 격려할 수 있도록 다소 관대하게 부여하세요.
                oneLineSummary는 절대 20자를 넘지 말아야 합니다.
                coreSentences는 반드시 여러 문장으로 구성하세요. 최소 15개 이상 작성하세요.
                각 항목은 turns의 userText에서 실제로 중요한 문장을 골라 originSentence, improvedSentence, keyExpression, keyExpressionKorean을 1:1:1:1로 대응시켜 주세요.
                coreSentences의 각 originSentence는 서로 다른 문장이어야 하며, 같은 문장을 중복해서 넣지 마세요.
                coreSentences의 originSentence는 반드시 turns의 userText 원문을 그대로 사용하세요.
                coreSentences의 각 항목에는 반드시 sourceTurnIndex를 포함하고, 이 값은 originSentence가 나온 turn의 index여야 합니다.
                keyExpression은 해당 ImprovedSentence문장에서 꼭 익혀야 할 핵심 표현 한 가지를 짧게 추출하세요.
                keyExpressionKorean은 keyExpression의 자연스러운 한글 뜻을 15자 이내로 짧게 설명하세요.
                keyExpression은 반드시 중급 이상(B2 이상) 표현만 선정하세요.
                explain, curious, nothing, hello, yes, no, okay, sorry, thank you 등 기초 단어(A1-A2)는 keyExpression으로 선정하지 마세요.
                관용구, 구동사(phrasal verb), 비즈니스 표현, 원어민이 자주 쓰는 자연스러운 표현 위주로 선정하세요.
                예: "hear me out", "board the plane", "on my mind", "that's the thing" 등
                habitAnalysis의 habit은 13자 이내로 작성하고, 해당 habit의 근거를 전달한 통화 내용에서 추출해야합니다. 해당 습관과 근거에 맞게 Suggestion또한 작성하세요.
                levelPercentage는 정수 퍼센트 값으로 작성하세요.
                levelAnalysis는 현재 영어 수준에 대한 짧은 해설을 작성하세요.
                weakPoints는 반드시 1개 이상 3개 이하의 태그를 선택하세요.
                weakPoints는 ["주어-동사 수 일치", "완료시제", "관사사용", "관계대명사 생략", "전치사 선택 오류"] 태그 안에서 선택하세요.
                환산표 (IELTS Speaking 기준):
                - IELTS 8~9 → OPIc: AL, TOEIC Speaking: 200
                - IELTS 7~8 → OPIc: AL, TOEIC Speaking: 180~200
                - IELTS 6.5~7 → OPIc: IH~AL, TOEIC Speaking: 160~180
                - IELTS 6~6.5 → OPIc: IM3~IH, TOEIC Speaking: 130~160
                - IELTS 5 → OPIc: IM2~IM3, TOEIC Speaking: 110~130
                - IELTS 4 → OPIc: IM1~IM2, TOEIC Speaking: 80~110
                - IELTS 3 → OPIc: IL~IM1, TOEIC Speaking: 50~80
                - IELTS 2 → OPIc: NH, TOEIC Speaking: 50 미만.
                 
                
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

    /**
     * AI 발화 텍스트 목록을 한국어로 일괄 번역.
     * 개별 호출 대신 한 번에 배열로 요청해서 API 비용 절감.
     */
    public List<String> translateTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        String systemPrompt = """
            당신은 영어를 한국어로 번역하는 전문 번역가입니다.
            반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.
            각 텍스트를 자연스러운 한국어로 번역하되, 원문의 뉘앙스와 말투를 유지하세요.
            
            {
              "translations": ["번역1", "번역2", "번역3"]
            }
            """;

        String userPrompt;
        try {
            userPrompt = objectMapper.writeValueAsString(Map.of("texts", texts));
        } catch (Exception e) {
            throw new RuntimeException("번역 요청 생성 실패: " + e.getMessage(), e);
        }

        String responseJson = callChatApi(systemPrompt, userPrompt);

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode translationsNode = root.path("translations");
            if (!translationsNode.isArray()) {
                throw new RuntimeException("번역 응답 형식 오류");
            }
            List<String> result = new java.util.ArrayList<>();
            for (JsonNode node : translationsNode) {
                result.add(node.asText());
            }
            return result;
        } catch (Exception e) {
            log.error("번역 응답 파싱 실패: {}", e.getMessage(), e);
            throw new RuntimeException("번역 응답 파싱 실패: " + e.getMessage(), e);
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
