package com.gymproject.classmanagement.recurrence.exception;

import com.gymproject.common.exception.BusinessException;

public class RecurrenceException extends BusinessException {

    // 에러 코드만 넘길 때
    public RecurrenceException(RecurrenceErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatusCode(),  errorCode.getErrorCode());
    }

    public RecurrenceException(RecurrenceErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage() + " (상세: %s)", args),
                errorCode.getStatusCode(),
                errorCode.getErrorCode());
    }
}
