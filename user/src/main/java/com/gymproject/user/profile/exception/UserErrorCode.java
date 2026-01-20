package com.gymproject.user.profile.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode {

    // 1. 조회 관련 (404)
USER_NOT_FOUND("사용자를 찾을 수 없습니다.", 404, "USER_NOT_FOUND"),

    // 2. 입력값 검증 관련 (400) - 호주 정책 반영
    INVALID_NAME_FORMAT("이름 형식이 올바르지 않습니다. (영문, 공백, 하이픈, 아포스트로피만 허용)" ,400, "INVALID_NAME_FORMAT"),
    INVALID_PHONE_FORMAT("전화번호 형식이 올바르지 않습니다. (호주 형식 +61 또는 04...)",400, "INVALID_PHONE_FORMAT" ),

    // 3. 중복 및 충돌 관련 (409)
    DUPLICATE_PHONE_NUMBER("이미 등록된 전화번호입니다.",409, "DUPLICATE_PHONE_NUMBER"),

    // 4. 기타 계정 상태
    USER_ALREADY_ACTIVE("이미 활성화된 사용자입니다.", 400, "ALREADY_ACTIVE" ),
    USER_DEACTIVATED("비활성화된 사용자입니다.", 403, "DEACTIVATED"),
    INVALID_FORMAT("최소 하나 이상의 수정 사항이 필요합니다", 400 ,"INVALID_FORMAT" ),;


    private final String message;
    private final int statusCode;
    private final String errorCode;
}
