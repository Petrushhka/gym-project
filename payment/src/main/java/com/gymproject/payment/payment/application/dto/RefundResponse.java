package com.gymproject.payment.payment.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "결제 환불 결과 응답")
public record RefundResponse(
        @Schema(description = "내부 결제 고유 ID (Payment ID)", example = "1001")
        Long paymentId,

        @Schema(description = "Stripe 환불 고유 식별자", example = "re_3NlX2...")
        String paymentIntentId,

        @Schema(description = "실제 환불된 금액 (AUD)", example = "450.00")
        Long refundedAmount,

        @Schema(description = "환불 처리 상태", example = "SUCCEEDED")
        String status,

        @Schema(description = "환불 완료 일시 (브리즈번 기준)", example = "2026-01-19T10:15:30+10:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Australia/Brisbane")
        OffsetDateTime refundedAt
) {
}
