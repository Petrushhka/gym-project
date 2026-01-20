package com.gymproject.common.exception.user;

import com.gymproject.common.exception.BusinessException;

public class InvalidCredentialsException extends BusinessException {
    public InvalidCredentialsException(String message) {
        super(message, 401, "INVALID_CREDENTIALS");
    }
}
