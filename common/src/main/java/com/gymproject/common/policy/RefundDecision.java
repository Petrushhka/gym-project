package com.gymproject.common.policy;

public record RefundDecision(
        boolean refundable,
        long refundAmount
) {}
