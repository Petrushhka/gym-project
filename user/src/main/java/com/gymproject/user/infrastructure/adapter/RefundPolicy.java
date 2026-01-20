package com.gymproject.user.infrastructure.adapter;

import com.gymproject.common.policy.RefundContext;
import com.gymproject.common.policy.RefundDecision;
import com.gymproject.common.port.payment.RefundPolicyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RefundPolicy implements RefundPolicyPort {

    private final List<RefundStrategy> strategies;

    @Override
    public RefundDecision decide(RefundContext context) {

        return strategies.stream()
                .filter(s -> s.supports(context.productCategory()))
                .findFirst()
                .map(s -> s.calculate(context))
                .orElse(new RefundDecision(false, 0L));
    }
}