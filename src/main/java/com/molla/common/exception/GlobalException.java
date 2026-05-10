package com.molla.common.exception;

import com.molla.common.response.ErrorCode;
import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {

    private final ErrorCode errorCode;

    public GlobalException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 동적 메시지가 필요한 경우 (예: "홍길동 유저를 찾을 수 없습니다.") */
    public GlobalException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
