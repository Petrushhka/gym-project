package com.gymproject.auth.exception;

import com.gymproject.common.exception.BusinessException;

public class IdentityException extends BusinessException {
    // 에러 코드만 넘길 때
    public IdentityException(IdentityErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatusCode(),  errorCode.getErrorCode());
    }

    public IdentityException(IdentityErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage() + " (상세: %s)", args),
                errorCode.getStatusCode(),
                errorCode.getErrorCode());
    }
}
