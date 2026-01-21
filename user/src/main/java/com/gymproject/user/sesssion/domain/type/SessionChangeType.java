package com.gymproject.user.sesssion.domain.type;

import lombok.Getter;

@Getter
public enum SessionChangeType {
    ISSUE("신규 지급"), // 세션권 신규 발급
    PURCHASE("구매"),       // 세션권 구매
    USE("사용"),            // 수업 예약으로 인한 차감
    RESTORE("복구"),         // 예약 취소로 인한 복구
    EXPIRED("만료"),        // 유효기간 경과
    DEACTIVATED("사용 정지"),// 멤버십 종료로 사용 불가
    REFUNDED("환불"); // 환불
    private final String description;

    SessionChangeType(String description) {
        this.description = description;
    }
}