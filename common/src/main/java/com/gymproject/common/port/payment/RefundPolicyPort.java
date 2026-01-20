package com.gymproject.common.port.payment;

import com.gymproject.common.policy.RefundContext;
import com.gymproject.common.policy.RefundDecision;

public interface RefundPolicyPort {

    RefundDecision decide(RefundContext context);

}
