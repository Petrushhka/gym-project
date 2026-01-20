package com.gymproject.common.exception.auth;

import com.gymproject.common.exception.BusinessException;

public class UnsupportedRegistrationIdException extends BusinessException {
    public UnsupportedRegistrationIdException(String message) {
        super(message, 400, "AUTH_PROVIDER_UNSUPPORTED");
    }
}
