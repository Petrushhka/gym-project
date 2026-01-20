package com.gymproject.user.sesssion.application;

import jakarta.validation.constraints.NotNull;

public record SessionPurchaseRequest(

        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId // Payment 모듈에 등록된 상품의 ID( 예: 55)

) {
}

/*
    세션은 구매일로터 사용하는 것임.
    따라서 type이런거 필요없음.
    startDate 도 필요없음.
 */