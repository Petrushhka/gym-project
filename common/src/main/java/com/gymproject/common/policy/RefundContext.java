package com.gymproject.common.policy;

import java.time.OffsetDateTime;

public record RefundContext(
        String productCategory,
        String productCode,
        String contractData,
        OffsetDateTime paidAt,
        Long paidAmount,
        Long sourceId
) {}
