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
            String encoding,
            String audio
    ) {
    }

    public record AssistantTurn(
            String text,
            OffsetDateTime createdAt
    ) {
    }
}
