package com.molla.domain.user;

import com.molla.common.response.ErrorCode;
import com.molla.controller.dto.auth.RegisterRequest;
import com.molla.controller.dto.user.UpdateUserRequest;
import com.molla.controller.dto.user.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getMe(String userId) {
        User user = findActiveUser(userId);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse register(String userId, RegisterRequest request) {
        User user = findActiveUser(userId);

        if (user.isRegistered()) {
            throw new UserException(ErrorCode.USER_ALREADY_REGISTERED);
        }

        user.register(request.username());

        log.info("회원가입 완료 — userId: {}, username: {}", userId, request.username());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMe(String userId, UpdateUserRequest request) {
        User user = findActiveUser(userId);
        user.update(request.username(), request.englishLevel());

        log.info("정보 수정 완료 — userId: {}", userId);
        return UserResponse.from(user);
    }

    @Transactional
    public void withdraw(String userId) {
        User user = findActiveUser(userId);
        user.withdraw();

        log.info("회원 탈퇴 완료 — userId: {}", userId);
    }

    private User findActiveUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if (user.isWithdrawn()) {
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.isSuspended()) {
            throw new UserException(ErrorCode.USER_SUSPENDED);
        }

        return user;
    }
}
