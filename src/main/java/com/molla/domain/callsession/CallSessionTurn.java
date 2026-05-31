package com.molla.domain.callsession;

import java.time.OffsetDateTime;

public record CallSessionTurn(
        Integer index,
        OffsetDateTime createdAt,
        UserTurn user,
        AssistantTurn assistant
) {
    public record UserTurn(
            String text,
            Integer sampleRate,
            String audioKey
    ) {
    }

    public record AssistantTurn(
            String text,
            String translatedText,   
            OffsetDateTime createdAt
    ) {
    }
}
