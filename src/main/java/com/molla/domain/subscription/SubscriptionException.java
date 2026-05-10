package com.molla.domain.subscription;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;

public class SubscriptionException extends GlobalException {

    public SubscriptionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SubscriptionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
