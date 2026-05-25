package com.molla.domain.feedbackreport;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record Report(
        String oneLineSummary,
        int levelPercentage,
        String levelAnalysis,
        List<CoreSentenceFeedback> coreSentences,
        List<HabitAnalysis> habitAnalyses,
        List<ReportScore> scores,
        List<String> weakPoints,
        String levelResult
) {
    public record CoreSentenceFeedback(
            @Schema(description = "원문이 나온 turn index", example = "3")
            Integer sourceTurnIndex,
            @Schema(description = "사용자가 실제로 말한 원문 문장", example = "She go to school")
            String originSentence,
            @Schema(description = "더 자연스럽게 다듬은 추천 문장", example = "She usually goes to school early in the morning.")
            String improvedSentence,
            @Schema(description = "꼭 익혀야 할 핵심 표현", example = "goes to school")
            String keyExpression,
            @Schema(description = "핵심 표현의 자연스러운 한글 뜻", example = "학교에 다니다")
            String keyExpressionKorean,
            @Schema(description = "원문 오디오 샘플레이트", example = "16000")
            Integer sampleRate,
            @Schema(description = "원문 오디오 S3 key", example = "calls/CA.../turns/5.wav")
            String audioKey,
            @Schema(description = "원문 오디오 재생용 presigned URL", example = "https://signed-url")
            String audioUrl
    ) {
    }

    public record HabitAnalysis(
            String habit,
            String evidence,
            String suggestion
    ) {
    }

    public record ReportScore(
            String exam,
            String score
    ) {
    }
}
