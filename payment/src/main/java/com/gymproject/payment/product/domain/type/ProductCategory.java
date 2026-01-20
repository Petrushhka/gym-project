package com.gymproject.payment.product.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductCategory {

    // 멤버십 상품
    MEMBERSHIP, SESSION;

    public static ProductCategory of(String productCategory) {
        if (productCategory == null || productCategory.isBlank()) {
            throw new IllegalArgumentException("ProductCategory is empty");
        }

        try {
            return ProductCategory.valueOf(productCategory.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ProductCategory: " + productCategory);
        }
    }
}
