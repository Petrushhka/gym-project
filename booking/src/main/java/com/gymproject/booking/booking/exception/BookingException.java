package com.gymproject.booking.booking.exception;

import com.gymproject.common.exception.BusinessException;

public class BookingException extends BusinessException {

    // 에러 코드만 넘길 때
    public BookingException(BookingErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatusCode(),  errorCode.getErrorCode());
    }

    public BookingException(BookingErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage() + " (상세: %s)", args),
                errorCode.getStatusCode(),
                errorCode.getErrorCode());
    }
}
