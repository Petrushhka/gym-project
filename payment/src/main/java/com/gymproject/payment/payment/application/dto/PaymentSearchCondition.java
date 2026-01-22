package com.gymproject.payment.payment.application.dto;

import com.gymproject.payment.payment.domain.type.PaymentStatus;
import com.gymproject.payment.product.domain.type.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "결제 내역 검색")
public record PaymentSearchCondition(
        @Schema(description = "결제 ID", example = "1005")
        Long paymentId,

        @Schema(description = "유저 ID (관리자만 사용 가능, 일반 유저는 본인 ID로 강제 고정)", example = "10")
        Long userId,

        @Schema(description = "결제 상태 (CAPTURED, REFUNDED 등)", example = "CAPTURED")
        PaymentStatus status,

        @Schema(description = "상품 카테고리 (MEMBERSHIP, SESSION)", example = "MEMBERSHIP")
        ProductCategory productCategory,

        @Schema(description = "조회 시작 날짜", example = "2026-02-01")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @Schema(description = "조회 종료 날짜", example = "2026-02-28")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate
) {
    // 일반 유저용 강제 변환 메서드 (보안용)
    public PaymentSearchCondition toUserScope(Long currentUserId) {
        return new PaymentSearchCondition(
                this.paymentId,
                currentUserId,
                this.status,
                this.productCategory,
                this.startDate,
                this.endDate
        );
    }
}
