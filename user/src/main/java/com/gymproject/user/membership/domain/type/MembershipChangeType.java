package com.gymproject.user.membership.domain.type;

public enum MembershipChangeType {
    PURCHASE("신규 구매"),
    EXTEND("기간 연장"),
    EXPIRED("기간 만료"),
    CANCELLED("전액 환불"),
    SUSPEND("일시 정지"),
    RESUME("일시 정지 해제"),
    ROLLBACK("환불");

    private final String description;

    MembershipChangeType(String description) {
        this.description = description;
    }
}
