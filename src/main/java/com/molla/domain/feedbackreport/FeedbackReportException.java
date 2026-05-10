package com.molla.domain.feedbackreport;

import com.molla.common.exception.GlobalException;
import com.molla.common.response.ErrorCode;

public class FeedbackReportException extends GlobalException {

    public FeedbackReportException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FeedbackReportException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
