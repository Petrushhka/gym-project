package com.gymproject.payment.application.port;

import com.gymproject.payment.application.dto.GatewayResponse;

public interface PaymentGatewayPort {

    // 1. 결제창 생성
    GatewayResponse createSession(
            Long userId, // 유저ID
            Long paymentId, // paymentID
            String productName, // 상품이름
            String planeName, // 플랜이름
            Long amount, // 가격
            String currency, // 통화(원, 달러)
            String successUrl, // 성공페이지
            String cancelUrl // 실패 페이지
    );

    // 2. 환불
    void refund(String paymentKey, Long amount);
}
