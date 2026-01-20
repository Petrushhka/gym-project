package com.gymproject.payment.product.exception;

import com.gymproject.common.exception.BusinessException;

public class ProductException extends BusinessException {

    // 에러 코드만 넘길 때
    public ProductException(ProductErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatusCode(),  errorCode.getErrorCode());
    }

    public ProductException(ProductErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage() + " (상세: %s)", args),
                errorCode.getStatusCode(),
                errorCode.getErrorCode());
    }
}
