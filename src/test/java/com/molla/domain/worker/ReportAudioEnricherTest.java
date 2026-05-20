package com.molla.domain.worker;

import com.molla.domain.callsession.CallSessionTurn;
import com.molla.domain.feedbackreport.Report;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportAudioEnricherTest {

    private final ReportAudioEnricher enricher = new ReportAudioEnricher();

    @Test
    void attachesMatchedTurnAudioToCoreSentences() {
        Report report = new Report(
                "좋은 대화였어요.",
                List.of(
                        new Report.CoreSentenceFeedback(2, "I received the wrong item.", "I received the wrong item.", "I received the wrong item, so I'd like an exchange.", null, null, null),
                        new Report.CoreSentenceFeedback(5, "She don't like spicy food.", "She doesn't like spicy food.", "She doesn't like spicy food, so we chose another place.", null, null, null)
                ),
                List.of(),
                List.of(),
                List.of(),
                null
        );

        List<CallSessionTurn> turns = List.of(
                new CallSessionTurn(
                        2,
                        OffsetDateTime.parse("2026-05-20T12:00:08.345678+00:00"),
                        new CallSessionTurn.UserTurn("I received the wrong item.", 16000, "pcm16le/base64", "AUDIO_2"),
                        new CallSessionTurn.AssistantTurn("Understood.", OffsetDateTime.parse("2026-05-20T12:00:09.456789+00:00"))
                ),
                new CallSessionTurn(
                        5,
                        OffsetDateTime.parse("2026-05-20T12:00:18.345678+00:00"),
                        new CallSessionTurn.UserTurn("She don't like spicy food.", 22050, "pcm16le/base64", "AUDIO_5"),
                        new CallSessionTurn.AssistantTurn("Okay.", OffsetDateTime.parse("2026-05-20T12:00:19.456789+00:00"))
                )
        );

        Report enriched = enricher.attachTurnAudio(report, turns);

        assertThat(enriched.coreSentences()).hasSize(2);
        assertThat(enriched.coreSentences().get(0).audio()).isEqualTo("AUDIO_2");
        assertThat(enriched.coreSentences().get(0).sampleRate()).isEqualTo(16000);
        assertThat(enriched.coreSentences().get(1).audio()).isEqualTo("AUDIO_5");
        assertThat(enriched.coreSentences().get(1).sampleRate()).isEqualTo(22050);
    }

    @Test
    void keepsCoreSentenceWhenMatchingTurnIsMissing() {
        Report report = new Report(
                "좋은 대화였어요.",
                List.of(
                        new Report.CoreSentenceFeedback(9, "Hello there.", "Hello there.", "Hello there. Nice to meet you.", null, null, null)
                ),
                List.of(),
                List.of(),
                List.of(),
                null
        );

        Report enriched = enricher.attachTurnAudio(report, List.of());

        assertThat(enriched.coreSentences().get(0).audio()).isNull();
        assertThat(enriched.coreSentences().get(0).encoding()).isNull();
    }
}
