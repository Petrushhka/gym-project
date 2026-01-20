package com.gymproject.common.exception;

public class JsonConvertFailedException extends BusinessException{
    public JsonConvertFailedException(String message, Throwable cause) {
        super(message, 500, "COMMON_JSON_002", cause);
    }
}
