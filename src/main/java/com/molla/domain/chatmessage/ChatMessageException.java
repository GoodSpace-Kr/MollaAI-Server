package com.molla.domain.chatmessage;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;

public class ChatMessageException extends GlobalException {

    public ChatMessageException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ChatMessageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
