package com.gymproject.user.membership.exception;

import com.gymproject.common.exception.BusinessException;

public class UserMembershipException extends BusinessException {

    // 에러 코드만 넘길 때
    public UserMembershipException(UserMembershipErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatusCode(),  errorCode.getErrorCode());
    }

    public UserMembershipException(UserMembershipErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage() + " (상세: %s)", args),
                errorCode.getStatusCode(),
                errorCode.getErrorCode());
    }
}
