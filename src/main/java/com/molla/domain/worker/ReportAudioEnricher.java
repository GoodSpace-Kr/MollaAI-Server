package com.molla.domain.worker;

import com.molla.domain.callsession.CallSessionTurn;
import com.molla.domain.feedbackreport.Report;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReportAudioEnricher {

    public Report attachTurnAudio(Report report, List<CallSessionTurn> turns) {
        if (report == null) {
            return null;
        }

        if (turns == null || turns.isEmpty() || report.coreSentences() == null || report.coreSentences().isEmpty()) {
            return report;
        }

        Map<Integer, CallSessionTurn> turnsByIndex = turns.stream()
                .filter(turn -> turn != null && turn.index() != null)
                .collect(Collectors.toMap(CallSessionTurn::index, Function.identity(), (left, right) -> left));

        List<Report.CoreSentenceFeedback> enrichedCoreSentences = report.coreSentences().stream()
                .map(coreSentence -> attachAudio(coreSentence, turnsByIndex.get(coreSentence.sourceTurnIndex())))
                .toList();

        return new Report(
                report.oneLineSummary(),
                report.levelPercentage(),
                report.levelAnalysis(),
                enrichedCoreSentences,
                report.habitAnalyses(),
                report.scores(),
                report.weakPoints(),
                report.levelResult()
        );
    }

    private Report.CoreSentenceFeedback attachAudio(
            Report.CoreSentenceFeedback coreSentence,
            CallSessionTurn matchedTurn
    ) {
        if (coreSentence == null) {
            return null;
        }

        if (matchedTurn == null || matchedTurn.user() == null) {
            return coreSentence;
        }

        return new Report.CoreSentenceFeedback(
                coreSentence.sourceTurnIndex(),
                coreSentence.originSentence(),
                coreSentence.improvedSentence(),
                coreSentence.keyExpression(),
                matchedTurn.user().sampleRate(),
                matchedTurn.user().audioKey(),
                null
        );
    }
}
