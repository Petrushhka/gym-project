package com.gymproject.payment.application.dto;

import com.gymproject.payment.product.domain.entity.Product;
import com.gymproject.payment.product.domain.type.ProductCategory;
import com.gymproject.payment.product.domain.type.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "상품 정보 응답")
public record ProductResponse(
        @Schema(description = "상품 ID", example = "1")
        Long productId,

        @Schema(description = "상품명", example = "브리즈번 헬스장 3개월 멤버십")
        String name,

        @Schema(description = "상품 카테고리 (MEMBERSHIP: 기간권, SESSION: 횟수권)", example = "MEMBERSHIP")
        ProductCategory category,

        @Schema(description = "판매 가격 (AUD)", example = "450.00")
        Long price,

        @Schema(description = "상품 코드", example = "MEMBERSHIP_3M")
        String code,

        @Schema(description = "판매 상태(ACTIVE: 판매중, INACTIVE: 판매중지)", example = "ACTIVE")
        ProductStatus status,

        @Schema(description = "등록 일자", example = "2026-01-18T10:00:00+11:00")
        OffsetDateTime createAt
) {
    public static ProductResponse create(Product product){
        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getCode(),
                product.getStatus(),
                product.getCreatedAt()
        );
    }
}
