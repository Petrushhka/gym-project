package com.gymproject.common.dto.payment;

public record ProductInfo(
        String name,
        Long price,
        String code
) {
}

// product -> memebership -> payment 순서