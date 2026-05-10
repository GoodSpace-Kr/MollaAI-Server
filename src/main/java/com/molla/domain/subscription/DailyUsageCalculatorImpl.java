package com.molla.domain.subscription;

import com.molla.domain.callsession.CallSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DailyUsageCalculatorImpl implements DailyUsageCalculator {

    private final CallSessionRepository callSessionRepository;

    @Override
    public int calculateTodayUsedMinutes(String userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        int totalSeconds = callSessionRepository.sumDurationSecondsTodayByUserId(userId, startOfDay);
        // 초 → 분 올림 (60초 미만도 1분으로 계산)
        return (int) Math.ceil(totalSeconds / 60.0);
    }
}
