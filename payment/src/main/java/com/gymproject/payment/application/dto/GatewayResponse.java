package com.gymproject.payment.application.dto;

public record GatewayResponse(
        String sessionUrl, // 결제창 URL
        String paymentKey // PG사 고유 ID (PaymentIntentId)
) {
}
