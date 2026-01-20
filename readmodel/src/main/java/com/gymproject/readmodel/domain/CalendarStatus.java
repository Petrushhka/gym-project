package com.gymproject.readmodel.domain;

import com.gymproject.common.event.domain.ScheduleEvent;
import com.gymproject.common.event.domain.TimeOffEvent;

public enum CalendarStatus {
//    // 1. 예약(Booking) 기반 상태
//    BOOKING_CONFIRMED,   // 1:1 / 그룹 모두: 확정된 예약
//    BOOKING_PENDING,     // 승인을 기다리는 예약 (무료체험)
//    BOOKING_CANCELLED,   // 예약 취소
//    BOOKING_REJECTED,
//    BOOKING_ATTENDED,
//    BOOKING_NOSHOW,

    // 1. 1:1PT 에약기반 상태
    RESERVED_TIME,

    // 2. 그룹 수업(Schedule) 상태
    CLASS_OPEN,          // 정원 남음 → 예약 가능
    CLASS_CLOSED,          // 정원 꽉 참 → 예약 불가, 수업은 진행
    CLASS_CANCELLED, // 수업 자체를 취소함 → 진행 X
    CLASS_FINISHED, // 수업이 종료됨

    // 3. 트레이너 휴무
    TIMEOFF_ACTIVE,      // 휴무 적용 중
    TIMEOFF_CANCELLED;    // 휴무 취소 -> 당장 사용 안함


    public static CalendarStatus mapStatus(TimeOffEvent event) {
        if(event.eventType() == null) return null;
        return switch (event.eventType()){
            case "REGISTERED" -> CalendarStatus.TIMEOFF_ACTIVE;
//            case "CANCELLED" -> CalendarStatus.TIMEOFF_CANCELLED;
            default -> null;
        };
    }

    /*
        [중요]
        ScheduleEvent에서 ActionType은 다음과같음
        CREATED, // 생성됨
        UPDATED, // 예약, 취소 등으로 정보 변경
        CANCELLED, // 폐강, 취소
        FINISHED

        Schedule에서 ScheduleStatus는 다음과 같음(이거 사용예정)

        OPEN,
        CLOSED,
        CANCELLED,
        RESERVED, // 1:1 예약이 들어온 상태(시간 점유 중인 상태, 대기든 아니든)
        FINISHED;
     */

    public static CalendarStatus mapStatus(ScheduleEvent event) {
        if(event.status() == null) return null;
        return switch (event.status()){
            case "OPEN" -> CalendarStatus.CLASS_OPEN;
            case "CLOSED" -> CalendarStatus.CLASS_CLOSED;
            case "CANCELLED" -> CalendarStatus.CLASS_CANCELLED;
            case "RESERVED" -> CalendarStatus.RESERVED_TIME;
            case "FINISHED" -> CalendarStatus.CLASS_FINISHED;
            default -> null;
        };
    }
}
