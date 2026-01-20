package com.gymproject.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IdentityErrorCode {
    // 404
    NOT_FOUND("해당 계정을 찾을 수 없습니다.", 404, "NOT_FOUND"),

    // 403
    EMAIL_VERIFICATION_REQUIRED("이메일 인증이 완료되지 않았습니다.", 403, "EMAIL_REQUIRED"),
    UNSUBSCRIBED("이미 탈퇴한 회원입니다.", 403, "ALREADY_UNSUBSCRIBED"),
    NOT_AUTHORITY("권한이 없는 사용자입니다.", 403, "NOT_AUTHORITY"),
    //400,
    INVALID_EMAIL("올바르지 않은 이메일 형식입니다.", 400, "INVALID_EMAIL"),
    INVALID_PASSWORD("비밀번호 형식이 옳바르지 않습니다.", 400, "PASSWORD_EMPTY"),
    INVALID_PARAM("프로필 정보는 필수입니다.", 400, "INVALID_PARAM"),
    PASSWORD_MISMATCH("비밀번호가 일치하지 않습니다.", 400, "PASSWORD_MISMATCH"), // 409
    ALREADY_LINKED("기존에 연동된 계정입니다.", 409, "ALREADY_LINKED"),
    DUPLICATE_EMAIL("기존에 가입된 계정입니다.", 409, "DUPLICATED"),


    //---------- 토큰 관련
    // 1. 가장 일반적인 토큰 오류 (서명이 틀리거나, 변조된 경우)
    INVALID_TOKEN("유효하지 않은 토큰입니다.", 401, "INVALID_TOKEN"),

    // 2. 만료된 토큰 (프론트에서 이 코드를 받으면 재로그인 또는 Refresh 시도)
    EXPIRED_TOKEN("만료된 토큰입니다. 다시 로그인해주세요.", 401, "EXPIRED_TOKEN"),

    // 3. 토큰 탈취 의심 (Redis의 RefreshToken과 클라이언트가 보낸 것이 다를 때)
    TOKEN_STOLEN("보안 위협이 감지되었습니다. 모든 기기에서 로그아웃됩니다.", 401, "TOKEN_STOLEN"),

    // 4. 토큰이 비어있거나 형식이 잘못된 경우 (400 Bad Request가 더 적절할 수 있음)
    MALFORMED_TOKEN("토큰 형식이 올바르지 않습니다.", 400, "MALFORMED_TOKEN"),

    // 5. 권한 부족 (토큰은 유효하나 해당 API에 접근 권한이 없는 경우)
    ACCESS_DENIED("해당 리소스에 접근할 권한이 없습니다.", 403, "ACCESS_DENIED"),
    PASSWORD_SAME_AS_CURRENT("변경하는 비밀번호가 현재 비밀번호와 같을 수 없습니다.", 403 ,"PASSWORD_SAME_AS_CURRENT" ),
    CODE_UNMATCHED("이메일 코드가 일치하지 않습니다.", 400 , "CODE_UNMATCHED"),;

    private final String message;
    private final int statusCode;
    private final String errorCode;

}
