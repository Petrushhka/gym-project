package com.gymproject.common.exception.user;

import com.gymproject.common.exception.BusinessException;

public class NotMemberException extends BusinessException {
    public NotMemberException(String message) {
        super(message, 403, "USER_NOT_MEMBER");
    }
}
// 정회원이 아님