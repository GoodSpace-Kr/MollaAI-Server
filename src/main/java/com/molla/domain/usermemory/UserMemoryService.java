package com.molla.domain.usermemory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMemoryService {

    private final UserMemoryRepository userMemoryRepository;

    @Transactional
    public void upsertMemory(
            String userId,
            String summary,
            String weakPoints,
            String habitPatterns,
            String interests,
            String goals,
            int addedMinutes
    ) {
        UserMemory memory = userMemoryRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserMemory newMemory = UserMemory.create(userId);
                    return userMemoryRepository.save(newMemory);
                });

        memory.update(summary, weakPoints, habitPatterns, interests, goals, addedMinutes);

        log.info("user_memories UPSERT 완료 — userId: {}, totalMinutes: {}",
                userId, memory.getTotalCallMinutes());
    }

    public UserMemory getMemory(String userId) {
        return userMemoryRepository.findByUserId(userId).orElse(null);
    }
}
