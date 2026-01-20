package com.gymproject.common.exception;

public class InvalidInputException extends BusinessException {
    public InvalidInputException(String message) {
        super(message, 400, "BAD_REQUEST");
    }
}
