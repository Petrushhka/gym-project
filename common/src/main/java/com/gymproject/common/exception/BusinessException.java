package com.gymproject.common.exception;

import lombok.Getter;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;

    // 기본 생성자
    protected BusinessException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    // casue를 super(message, cause)로 전달
    protected BusinessException(String message, int statusCode, String errorCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
}

/*
    private final HttpStatus httpStatus << 사용하지 않는 이유

    spring-web 의존성이 common 모듈에 걸리기 때문임

    해당 의존성이 있으면 안되는 이유는 아래 블로그 주소 참고
    https://petrushka3.tistory.com/116
 */

/*
    [공부]
    cause를 부모인 RuntimeException까지 전달해줘야 나중에 로그를 찍었을 때
    그 안에 내용을보고 위치를 정확히 알 수 있음.
 */