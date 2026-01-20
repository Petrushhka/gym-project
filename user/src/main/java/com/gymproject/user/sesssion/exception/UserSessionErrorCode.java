package com.gymproject.user.sesssion.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSessionErrorCode {
    // 404
    NOT_FOUND("해당 세션권을 찾을 수 없습니다.", 404, "SESSION_404"),
   //403
    EXPIRED("만료된 세션권입니다.", 403, "SESSION_EXPIRED"),
    EXHAUSTED("남은 세션 횟수가 없습니다.", 403, "SESSION_EXHAUSTED"),
   //400
    INVALID_STATUS("세션권의 상태를 확인해주세요.", 400, "SESSION_INVALID_STATUS"),
    RESTORE_NOT_ALLOWED("복구할 수 없는 상태이거나 내역이 없습니다.", 400, "SESSION_RESTORE_FAILED"),
    ALREADY_REFUNDED("이미 환불 처리된 세션입니다.", 400, "SESSION_ALREADY_REFUNDED"),
    DEACTIVATED("정지된 세션권입니다.", 400, "SESSION_DEACTIVATED"),
    ALREADY_USED("이미 사용된 세션권 입니다.", 400 , "SESSION_ALREADY_USED"),
    INVALID_PRODUCT_TYPE("작업을 수행할 수 없는 상품입니다.", 400, "INVALID_PRODUCT_TYPE" ),
    INVALID_DATE_RANGE("종료일이 시작일보다 이전일 수 없습니다.", 400 , "INVALID_DATE_RANGE" ),
    INVALID_DATE_FORMAT("시작일과 종료일은 필수입니다.", 400 , "INVALID_DATE_FORMAT" ),;

    private final String message;
    private final int statusCode;
    private final String errorCode;

}
