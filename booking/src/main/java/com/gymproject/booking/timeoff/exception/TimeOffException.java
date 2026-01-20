package com.gymproject.booking.timeoff.exception;

import com.gymproject.common.exception.BusinessException;

public class TimeOffException extends BusinessException {

    // 에러 코드만 넘길 때
    public TimeOffException(TimeOffErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatusCode(),  errorCode.getErrorCode());
    }

    public TimeOffException(TimeOffErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage() + " (상세: %s)", args),
                errorCode.getStatusCode(),
                errorCode.getErrorCode());
    }
}
