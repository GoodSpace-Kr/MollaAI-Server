package com.molla.domain.conversationturn;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;

public class ConversationTurnException extends GlobalException {

    public ConversationTurnException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConversationTurnException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
