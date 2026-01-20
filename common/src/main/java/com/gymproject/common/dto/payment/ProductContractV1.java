package com.gymproject.common.dto.payment;

import java.time.OffsetDateTime;
///  [중요]
/// 결제 시점에서 기록을 스냅샷으로 남기는 용도
/// RefundContext는 환불 요청 시점에서 결제 정보를 담는 용도임

public record ProductContractV1(
        int version, // 버전
        String type,               // 구매 타입 (NEW/EXTEND)
        String productName,        // 상품명
        OffsetDateTime startDate,  // 시작일
        OffsetDateTime endDate,    // 종료일 (멤버십인 경우)
        Integer sessionCount       // 횟수 (PT인 경우)
) {
}
