package com.gymproject.payment.product.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode {

    // 404: Not Found
    PRODUCT_NOT_FOUND("상품 정보를 찾을 수 없습니다.", 404, "PRODUCT_NOT_FOUND"),


    // 가격이 음수일 때
    PRODUCT_INVALID_PRICE("상품 가격은 0원 이상이어야 합니다.", 400, "PRODUCT_INVALID_PRICE"),

    // 이름이나 코드가 비어있을 때
    INVALID_PRODUCT_DATA("상품 이름과 코드는 필수입니다.", 400, "INVALID_PRODUCT_DATA"),

    PRODUCT_ALREADY_DELETED("이미 삭제된 상품입니다.", 400, "PRODUCT_ALREADY_DELETED"),

    PRODUCT_ALREADY_EXISTS("이미 존재하는 상품 코드입니다.", 409, "PRODUCT_ALREADY_EXISTS"),
    ;


    private final String message;
    private final int statusCode;
    private final String errorCode;
}
