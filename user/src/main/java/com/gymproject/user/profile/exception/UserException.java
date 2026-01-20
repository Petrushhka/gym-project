package com.gymproject.user.profile.exception;

import com.gymproject.common.exception.BusinessException;

public class UserException extends BusinessException {

    public UserException(UserErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatusCode(), errorCode.getErrorCode());
    }

    public UserException(UserErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage() + "(상세: %s", args), // eg. 만료된 세션권입니다. 상세: args
                errorCode.getStatusCode(),
                errorCode.getErrorCode());
    }
}
