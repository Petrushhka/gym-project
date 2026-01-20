package com.gymproject.booking.booking.domain.type;

public enum CancellationType {
    FREE_CANCEL, // 무료 취소
    PENALTY_CANCEL, // 위약금 취소(세션 감소)
    IMPOSSIBLE // 취소 불가

}
