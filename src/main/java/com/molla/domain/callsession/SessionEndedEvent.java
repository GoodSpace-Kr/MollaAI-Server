package com.molla.domain.callsession;

import lombok.Getter;

/**
 * 통화 세션 종료 시 발행되는 Spring Application Event.
 * 리포트 생성 워커가 이 이벤트를 수신해서 비동기 처리.
 */
@Getter
public class SessionEndedEvent {

    private final String sessionId;
    private final String userId;
    private final boolean isLevelTest;

    public SessionEndedEvent(String sessionId, String userId, boolean isLevelTest) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.isLevelTest = isLevelTest;
    }
}
