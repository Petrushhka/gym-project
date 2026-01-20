package com.gymproject.user.membership.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "결제 페이지 생성 응답")
public record CheckoutResponse(
        @Schema(description = "Stripe 결제 페이지 URL (프론트엔드에서 리다이렉트할 주소)",
                example = "https://checkout.stripe.com/c/pay/cs_test_...")
        String paymentUrl

) {
}
