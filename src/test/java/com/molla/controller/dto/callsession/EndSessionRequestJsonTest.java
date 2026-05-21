package com.molla.controller.dto.callsession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.molla.domain.callsession.CallSessionTurn;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EndSessionRequestJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void deserializesTurnsAndRendersTranscript() throws Exception {
        String json = """
                {
                  "status": "completed",
                  "turns": [
                    {
                      "index": 1,
                      "createdAt": "2026-05-20T12:00:01.123456+00:00",
                      "user": {
                        "text": "hello",
                        "sampleRate": 16000,
                        "audioKey": "calls/test/turn-1.wav"
                      },
                      "assistant": {
                        "text": "hi there",
                        "createdAt": "2026-05-20T12:00:02.234567+00:00"
                      }
                    }
                  ]
                }
                """;

        EndSessionRequest request = objectMapper.readValue(json, EndSessionRequest.class);
        CallSessionTurn callSessionTurn = request.toCallSessionTurns().get(0);

        assertThat(request.status()).isEqualTo("completed");
        assertThat(request.turns()).hasSize(1);

        EndSessionRequest.TurnPayload turn = request.turns().get(0);
        assertThat(turn.index()).isEqualTo(1);
        assertThat(turn.user()).isNotNull();
        assertThat(turn.assistant()).isNotNull();
        assertThat(turn.user().text()).isEqualTo("hello");
        assertThat(turn.user().sampleRate()).isEqualTo(16000);
        assertThat(turn.user().audioKey()).isEqualTo("calls/test/turn-1.wav");
        assertThat(turn.assistant().text()).isEqualTo("hi there");
        assertThat(callSessionTurn.user().text()).isEqualTo("hello");
        assertThat(callSessionTurn.assistant().text()).isEqualTo("hi there");
    }

    @Test
    void convertsTurnsWithoutDroppingBlankAudioMetadata() throws Exception {
        String json = """
                {
                  "status": "completed",
                  "turns": [
                    {
                      "index": 1,
                      "createdAt": "2026-05-20T12:00:01.123456+00:00",
                      "user": {
                        "text": "I received the wrong item.",
                        "sampleRate": 16000,
                        "audioKey": "calls/test/turn-2.wav"
                      },
                      "assistant": {
                        "text": "Understood. I'll help you with that.",
                        "createdAt": "2026-05-20T12:00:02.234567+00:00"
                      }
                    },
                    {
                      "index": 2,
                      "createdAt": "2026-05-20T12:00:08.345678+00:00",
                      "user": {
                        "text": " ",
                        "sampleRate": 16000,
                        "audioKey": "calls/test/turn-3.wav"
                      },
                      "assistant": {
                        "text": null,
                        "createdAt": "2026-05-20T12:00:09.456789+00:00"
                      }
                    }
                  ]
                }
                """;

        EndSessionRequest request = objectMapper.readValue(json, EndSessionRequest.class);
        assertThat(request.toCallSessionTurns()).hasSize(2);

        CallSessionTurn firstTurn = request.toCallSessionTurns().get(0);
        CallSessionTurn secondTurn = request.toCallSessionTurns().get(1);

        assertThat(firstTurn.user().text()).isEqualTo("I received the wrong item.");
        assertThat(firstTurn.assistant().text()).isEqualTo("Understood. I'll help you with that.");
        assertThat(secondTurn.user().audioKey()).isEqualTo("calls/test/turn-3.wav");
        assertThat(secondTurn.assistant().text()).isNull();
    }
}
