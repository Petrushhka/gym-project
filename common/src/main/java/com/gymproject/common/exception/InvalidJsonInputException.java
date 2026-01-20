package com.gymproject.common.exception;

public class InvalidJsonInputException extends BusinessException{
    public InvalidJsonInputException(String message) {
        super(message, 400, "COMMON_JSON_001");
    }
}
