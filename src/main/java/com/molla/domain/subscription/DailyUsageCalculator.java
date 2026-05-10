package com.molla.domain.subscription;

/**
 * 오늘 사용한 통화 분을 계산하는 인터페이스.
 *  Spring 빈 순환 참조가 발생 방지
 * 구현체는 CallSession 도메인에서 제공 (6단계).
 */
public interface DailyUsageCalculator {

    /**
     * 오늘(00:00 ~ 현재) 완료된 통화 세션의 총 사용 분 반환.
     *
     * @param userId 유저 ID
     * @return 오늘 사용한 총 분 (초 → 분 올림)
     */
    int calculateTodayUsedMinutes(String userId);
}
