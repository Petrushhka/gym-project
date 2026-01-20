package com.gymproject.common.exception.auth;

import com.gymproject.common.exception.BusinessException;

public class OAuthCommunicationException extends BusinessException {
    public OAuthCommunicationException(String message, Throwable cause)
    {
        super(message, 502, "OAUTH_COMMUNICATION_ERROR", cause);
    }
}
