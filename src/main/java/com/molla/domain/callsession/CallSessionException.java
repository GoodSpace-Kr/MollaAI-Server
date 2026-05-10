package com.molla.domain.callsession;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;

public class CallSessionException extends GlobalException {

    public CallSessionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CallSessionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
