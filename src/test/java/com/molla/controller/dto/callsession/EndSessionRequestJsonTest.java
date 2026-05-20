package com.molla.controller.dto.callsession;

import com.fasterxml.jackson.databind.ObjectMapper;
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
                        "encoding": "pcm16le/base64",
                        "audio": "aGVsbG8="
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

        assertThat(request.status()).isEqualTo("completed");
        assertThat(request.turns()).hasSize(1);
        assertThat(request.renderTranscript()).isEqualTo("USER: hello\nAI: hi there");

        EndSessionRequest.TurnPayload turn = request.turns().get(0);
        assertThat(turn.index()).isEqualTo(1);
        assertThat(turn.user()).isNotNull();
        assertThat(turn.assistant()).isNotNull();
        assertThat(turn.user().text()).isEqualTo("hello");
        assertThat(turn.user().sampleRate()).isEqualTo(16000);
        assertThat(turn.user().encoding()).isEqualTo("pcm16le/base64");
        assertThat(turn.user().audio()).isEqualTo("aGVsbG8=");
        assertThat(turn.assistant().text()).isEqualTo("hi there");
    }

    @Test
    void renderTranscriptSkipsEmptyTurns() throws Exception {
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
                        "encoding": "pcm16le/base64",
                        "audio": "Zm9v"
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
                        "encoding": "pcm16le/base64",
                        "audio": "YmFy"
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

        assertThat(request.renderTranscript()).isEqualTo("""
                USER: I received the wrong item.
                AI: Understood. I'll help you with that.
                """.trim());
    }
}
