package com.gymproject.payment.payment.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "결제 환불 요청")
public class RefundRequest {

    @NotBlank
    @Schema(description = "환불 대상 주문(결제) 고유 번호", example = "2")
    private String paymentId;

}
