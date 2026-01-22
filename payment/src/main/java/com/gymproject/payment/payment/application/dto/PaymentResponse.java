package com.gymproject.payment.payment.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gymproject.payment.payment.domain.entity.Payment;
import com.gymproject.payment.payment.domain.type.PaymentStatus;
import com.gymproject.payment.product.domain.type.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "결제 내역 상세 응답")
public record PaymentResponse(
        @Schema(description = "결제 ID", example = "1005")
        Long paymentId,

        @Schema(description = "결제 금액 (단위: AUD)", example = "50000")
        Long amount,

        @Schema(description = "결제 상태", example = "CAPTURED")
        PaymentStatus status,

        @Schema(description = "상품명 (스냅샷)", example = "여름 할인 1개월권")
        String productName,

        @Schema(description = "상품 종류", example = "MEMBERSHIP")
        ProductCategory productCategory,

        @Schema(description = "결제 일시 (호주 브리즈번 기준)", example = "2026-02-15 14:30:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Australia/Brisbane")
        OffsetDateTime createdAt,

        @Schema(description = "최종 수정 일시 (환불 등)", example = "2026-02-16 09:00:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Australia/Brisbane")
        OffsetDateTime updatedAt
) {
    public static PaymentResponse create(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .amount(payment.getAmountCents())
                .status(payment.getStatus())
                .productName(payment.getSnapshotProductName())
                .productCategory(payment.getSnapshotProductCategory())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}