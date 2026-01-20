package com.gymproject.auth.exception;

import com.gymproject.common.exception.BusinessException;

public class OauthException extends BusinessException {
    // 에러 코드만 넘길 때
    public OauthException(OauthErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatusCode(),  errorCode.getErrorCode());
    }

    public OauthException(OauthErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage() + " (상세: %s)", args),
                errorCode.getStatusCode(),
                errorCode.getErrorCode());
    }
}
