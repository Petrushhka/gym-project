package com.gymproject.classmanagement.schedule.exception;

import com.gymproject.common.exception.BusinessException;

public class ScheduleException extends BusinessException {

    // 에러 코드만 넘길 때
    public ScheduleException(ScheduleErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatusCode(),  errorCode.getErrorCode());
    }

    public ScheduleException(ScheduleErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage() + " (상세: %s)", args),
                errorCode.getStatusCode(),
                errorCode.getErrorCode());
    }
}
