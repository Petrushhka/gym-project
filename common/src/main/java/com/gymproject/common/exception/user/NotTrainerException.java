package com.gymproject.common.exception.user;

import com.gymproject.common.exception.BusinessException;

public class NotTrainerException extends BusinessException {
    public NotTrainerException(String message) {
        super(message, 403, "USER_NOT_TRAINER");
    }
}
