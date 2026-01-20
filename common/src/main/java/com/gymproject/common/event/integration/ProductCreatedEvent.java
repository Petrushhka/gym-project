package com.gymproject.common.event.integration;

public record ProductCreatedEvent(Long paymentId,
                                  Long sourceId) {
}

/*
    CLass 타입에서 Record로 변경
    불변객체 전달용은 Java 17이상에서는 record를 사용하는 것이 추천됨.
 */