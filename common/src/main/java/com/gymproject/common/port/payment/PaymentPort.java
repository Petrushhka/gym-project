package com.gymproject.common.port.payment;

import java.time.OffsetDateTime;

public interface PaymentPort {
    String readyToPayMembership(
            Long userId, // 사용자 아이디
            String productName, // 상품이름
            String productCode, // 상품코드
            Long amount,// 결제코드
            OffsetDateTime startDate, // 시작일자
            OffsetDateTime endDate, // 종료일자
            String type // 타입
    );

    String readyToPaySession(
            Long userId,
            String productName,
            String productCode,
            Long amount,
            Integer totalSessionCount, // 횟수
            OffsetDateTime startDate,
            OffsetDateTime endDate
    );

    void compensate(String paymentKey);
}
