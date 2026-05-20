package com.molla.domain.worker;

import com.molla.domain.callsession.CallSessionTurn;

import java.util.List;

public record ReportTurnInput(
        Integer index,
        String userText,
        String assistantText
) {
    public static List<ReportTurnInput> from(List<CallSessionTurn> turns) {
        return turns.stream()
                .map(turn -> new ReportTurnInput(
                        turn.index(),
                        turn.user() != null ? turn.user().text() : null,
                        turn.assistant() != null ? turn.assistant().text() : null
                ))
                .toList();
    }
}
