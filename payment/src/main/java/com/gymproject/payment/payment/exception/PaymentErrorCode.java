package com.gymproject.payment.payment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode {

    // 404: Not Found
    PAYMENT_NOT_FOUND("결제 정보를 찾을 수 없습니다.", 404, "PAYMENT_NOT_FOUND"),

    // 400: Bad Request (유효성 검사 실패, 정책 위반)
    INVALID_PAYMENT_STATUS("유효하지 않은 결제 상태입니다.", 400, "PAYMENT_INVALID_STATUS"),
    ALREADY_PROCESSED("이미 처리된 결제입니다.", 400, "PAYMENT_ALREADY_PROCESSED"),
    REFUND_NOT_ALLOWED("환불 가능한 상태가 아닙니다.", 400, "PAYMENT_REFUND_NOT_ALLOWED"),
    PAYMENT_ALREADY_BOUND("이미 상품이 연결된 결제 내역입니다.", 409, "PAYMENT_ALREADY_BOUND"),
    // [수정] 500 -> 400 (정책 거절은 서버 에러가 아님)
    REFUND_REJECTED("환불 정책에 의해 거절되었습니다.", 400, "PAYMENT_REFUND_REJECTED"),

    // [추가] Service에서 Processor 찾을 때 필요
    INVALID_PRODUCT_TYPE("지원하지 않는 상품 타입입니다.", 400, "PAYMENT_INVALID_PRODUCT_TYPE"),

    // 403: Forbidden
    // [수정] Enum 이름과 에러 코드 일치 (NOT_YOUR_PAYMENT -> ACCESS_DENIED)
    ACCESS_DENIED("본인의 결제 내역만 접근 가능합니다.", 403, "PAYMENT_ACCESS_DENIED"),

    // 502: Bad Gateway (외부 시스템 연동 오류)
    // 500보다는 502가 '외부 PG사 문제'임을 명확히 보여줌
    PG_ERROR("PG사 연동 중 오류가 발생했습니다.", 502, "PAYMENT_PG_ERROR");


    private final String message;
    private final int statusCode;
    private final String errorCode;
}
