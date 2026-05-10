package com.molla.domain.auth;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;

public class AuthException extends GlobalException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
