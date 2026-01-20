package com.gymproject.user.profile.domain.type;

public enum UserSessionStatus {
    ACTIVE,      // 사용 가능
    EXPIRED,     // 만료됨
    FULLY_USED,  // 모두 소진
    CANCELED,     // 취소 처리된 세션
    DEACTIVATED, // 사용 불가 (멤버십 만료)
    REFUNDED // 환불된 세션
}