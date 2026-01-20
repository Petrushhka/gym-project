package com.gymproject.classmanagement.recurrence.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecurrenceType {
    CURRICULUM("과정형(묶음)", true, true),      // 커리큘럼 기반 — 중간참여 불가
    ROUTINE("단과형(개별)", false, false),// 독립 세션 — 중간참여 가능
    PERSONAL("개인수업", false, false); // 개인수업
    // 이후에 HYBRID 등 추가(2주 필참 후 자유예약 등)

    private final String description;

    private final boolean hasSharedCapacity; // 부모랑 좌석이 공유되어야하는지?(RecurrenceGrop이랑)
    private final boolean requireBatchBooking; // 한번에 그룹단위 예약이 필수로 되어야하는지?

    public boolean isHasSharedCapacity() {
        return hasSharedCapacity;
    }

    public boolean isRequireBatching() {
        return requireBatchBooking;
    }
}
