package com.gymproject.user.membership.domain.type;

public enum MembershipStatus {
    ACTIVE, // 현재 유효
    EXPIRED, // 기간 만료
    CANCELLED, // 중도 해지
    SUSPENDED // 일시 정지(관리자에 의해, 또는 본인이)
}

/**
 * ACTVIE -> EXPIRED (시간 경과)
 * ACTIVE -> CANCELLED,(환불)
 * ACTIVE -> SUSPENED (일시 정지)
 * SUSPENDED -> ACTIVC(활성화)
 *
 * EXPIRED -> ACTIVE (새로 결제)
 */