package com.molla.domain.wordbookmark;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;

public class WordBookmarkException extends GlobalException {

    public WordBookmarkException(ErrorCode errorCode) {
        super(errorCode);
    }

    public WordBookmarkException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
