package com.gymproject.booking.booking.domain.policy;

import com.gymproject.booking.booking.domain.type.BookingStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookingConfirmPolicy {
    AUTO_CONFIRM(BookingStatus.CONFIRMED), // 자동 확정
    REQUIRE_APPROVAL(BookingStatus.PENDING); // 대기

    private final BookingStatus initialStatus;
}
// 예약이
// 1)자동 확정이냐
// 2) 승인필요냐
