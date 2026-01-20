package com.gymproject.payment.payment.application.dto;

public record PaymentRequest(
        Long userId, // 누가
        Long productId, // 무엇을 (상품ID)
        Long amount, // 가격
        String paymentKey, // PG사 결제 고유 번호
        Long paymentId // 우리 DB의 PAYMENT_RECORD_TB 고유 ID
) {
}
