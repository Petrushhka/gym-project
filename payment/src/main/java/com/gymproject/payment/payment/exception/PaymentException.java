package com.gymproject.payment.payment.exception;

import com.gymproject.common.exception.BusinessException;

public class PaymentException extends BusinessException {

    // 에러 코드만 넘길 때
    public PaymentException(PaymentErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatusCode(),  errorCode.getErrorCode());
    }

    public PaymentException(PaymentErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage() + " (상세: %s)", args),
                errorCode.getStatusCode(),
                errorCode.getErrorCode());
    }
}
