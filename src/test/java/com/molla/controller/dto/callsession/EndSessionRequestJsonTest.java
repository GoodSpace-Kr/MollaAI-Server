package com.molla.controller.dto.callsession;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EndSessionRequestJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesTranscriptAndUtterances() throws Exception {
        String json = """
                {
                  "status": "completed",
                  "transcript": "USER: hello\\nAI: hi there",
                  "utterances": [
                    {
                      "text": "hello",
                      "sampleRate": 16000,
                      "encoding": "pcm16le/base64",
                      "audio": "aGVsbG8="
                    }
                  ]
                }
                """;

        EndSessionRequest request = objectMapper.readValue(json, EndSessionRequest.class);

        assertThat(request.status()).isEqualTo("completed");
        assertThat(request.transcript()).isEqualTo("USER: hello\nAI: hi there");
        assertThat(request.utterances()).hasSize(1);

        EndSessionRequest.UtterancePayload utterance = request.utterances().get(0);
        assertThat(utterance.text()).isEqualTo("hello");
        assertThat(utterance.sampleRate()).isEqualTo(16000);
        assertThat(utterance.encoding()).isEqualTo("pcm16le/base64");
        assertThat(utterance.audio()).isEqualTo("aGVsbG8=");
    }
}
