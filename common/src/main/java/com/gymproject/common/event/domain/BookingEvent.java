package com.gymproject.common.event.domain;

import java.time.OffsetDateTime;

public record BookingEvent(
        Long bookingId, // sourceId
        Long trainerId, // Schedule에서 가져와야함
        Long scheduleId, // Schedule에서 가져와야함
        OffsetDateTime startAt,// Schedule에서 가져와야함
        OffsetDateTime endAt, // Schedule에서 가져와야함
        String eventType, // APPROVCE, CANCEL 등 (Booking.ActionType)
        String previousStatus,
        String newStatus,
        String reason,
        Long userSessionId,
        String cancellationType,
        String bookingType
        ) {

    // 1:1 예약인지확인
    public boolean isPersonalBooking(){
        return this.bookingType.equalsIgnoreCase( "PERSONAL");
    }
    // 그룹 수업(커리큘럼 or 루틴)인지 확인
    public boolean isGroupBooking(){
        return this.bookingType.equalsIgnoreCase("GROUP_ROUTINE")
                || this.bookingType.equalsIgnoreCase( "GROUP_CURRICULUM");
    }
}

/*
    [패키지]
    domain: 비지니스 상태를 알리는 이벤트
    integration: 결제 완료, 상품 생성 등 시스템 간 흐름을 제어하는 이벤트
 */