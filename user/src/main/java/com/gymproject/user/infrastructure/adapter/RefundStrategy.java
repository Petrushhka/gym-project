package com.gymproject.user.infrastructure.adapter;

import com.gymproject.common.policy.RefundContext;
import com.gymproject.common.policy.RefundDecision;

public interface RefundStrategy {
    // 내가 처리할 수 있는 카테고리인지 확인
    boolean supports(String productCategory);

    // 실제 환불 금액 계산 수행
    RefundDecision calculate(RefundContext context);
}
