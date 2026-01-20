package com.gymproject.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OauthErrorCode {
    // 400: 클라이언트 요청 오류 또는 잘못된 데이터
    INVALID_PROVIDER("지원하지 않는 소셜 로그인 제공자입니다.", 400, "INVALID_PROVIDER"),

    // 404: 데이터 없음
    NOT_FOUND("연동된 소셜 계정을 찾을 수 없습니다.", 404, "NOT_FOUND"),

    // 409: 비즈니스 충돌 (중복 연동 등)
    ALREADY_LINKED("해당 소셜 계정은 이미 다른 사용자에게 연동되어 있습니다.", 409, "ALREADY_LINKED"),

    // 502: 외부 서비스(Google/Apple) 통신 및 응답 오류
    INVALID_EXTERNAL_RESPONSE("외부 인증 기관으로부터 올바른 응답을 받지 못했습니다.", 502, "INVALID_EXTERNAL_RESPONSE"),
    EXTERNAL_COMMUNICATION_ERROR("외부 인증 기관과의 통신에 실패했습니다.", 502, "EXTERNAL_COMMUNICATION_ERROR"),;

    private final String message;
    private final int statusCode;
    private final String errorCode;

}
