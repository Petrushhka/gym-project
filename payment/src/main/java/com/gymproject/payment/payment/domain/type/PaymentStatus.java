package com.gymproject.payment.payment.domain.type;

public enum PaymentStatus {
    PENDING, // 결제 요청 생성
    CAPTURED, // 웹훅으로 결제 성공 응답
    FAILED, // 웹훅으로 결제 실패 응답
    CANCELED, // 사용자 취소
    REFUNDED, // 환불
    REFUND_REQUEST, //환불 요청
    REFUND_FAILED // 환불 실패
}
