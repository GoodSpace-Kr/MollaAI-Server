package com.molla.domain.feedbackreport;

import java.util.List;

public record Report(
        String oneLineSummary,
        List<CoreSentenceFeedback> coreSentences,
        List<HabitAnalysis> habitAnalyses,
        List<ReportScore> scores,
        List<String> weakPoints,
        String levelResult
) {
    public record CoreSentenceFeedback(
            Integer sourceTurnIndex,
            String sentence,
            String grammarCorrection,
            String improvedSentence,
            Integer sampleRate,
            String audioKey,
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
