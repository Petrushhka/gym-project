package com.gymproject.payment.payment.application.service.processor;

import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.policy.RefundDecision;
import com.gymproject.payment.payment.domain.entity.Payment;
import com.gymproject.payment.product.domain.type.ProductCategory;

public interface RefundProcessor {

    boolean supports(ProductCategory category);

    RefundDecision process(Payment payment, UserAuthInfo userAuthInfo);

}
