package com.gymproject.payment.product.application.dto;

import com.gymproject.payment.product.domain.type.ProductCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "상품 등록 요청")
public record CreateProductRequest (
        @NotBlank(message = "상품명은 필수입니다.")
        @Schema(description = "상품명", example = "3개월 무제한 멤버십")
        String name,

        @NotNull(message = "가격은 필수입니다.")
        @Positive(message = "가격은 0원보다 커야 합니다.")
        @Schema(description = "판매 가격 (AUD 기준, 센트 단위 아님)", example = "450")
        Long price,

        @NotNull(message = "카테고리는 필수입니다.")
        @Schema(description = "상품 카테고리 (MEMBERSHIP, SESSION)", example = "MEMBERSHIP")
        ProductCategory category,

        @NotBlank(message = "상품 코드는 필수입니다.")
        @Schema(description = """
        내부 관리용 고유 코드, 프론트에서 유저멤버십과 세션과 관련된 상품 코드를 미리 가지고 있어야함.
        추후 사용자가 상품을 코드와 상품 등록과 수정에 있어서 좀 더 엔티티를 확장할 예정
        """, example = "MEMBERSHIP_3M")
        String code // 여기에 "MEMBERSHIP_3M" 등을 입력
){
}
