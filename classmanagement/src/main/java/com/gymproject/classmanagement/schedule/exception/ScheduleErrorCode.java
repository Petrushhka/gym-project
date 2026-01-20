package com.gymproject.classmanagement.schedule.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode {

    // 404: Not Found
    NOT_FOUND("해당 수업 스케줄을 찾을 수 없습니다.", 404, "SCHEDULE_NOT_FOUND"),

    // 400: Business Logic (State)
    INVALID_STATUS("현재 수업 상태에서는 해당 작업을 수행할 수 없습니다.", 400, "SCHEDULE_INVALID_STATUS"),
    ALREADY_CLOSED_OR_FINISHED("이미 종료되거나 취소된 수업입니다.", 400, "SCHEDULE_ALREADY_CLOSED"),
    ALREADY_RESERVED_PERSONAL("1:1 개인 수업은 별도의 예약/취소 로직을 타지 않습니다.", 400, "SCHEDULE_PERSONAL_IMMUTABLE"),

    // Reservation Validation (Policy)
    RESERVATION_NOT_ALLOWED_CURRICULUM("커리큘럼(과정)형 수업은 개별 회차 예약이 불가능합니다.", 400, "SCHEDULE_CURRICULUM_RESERVATION_NOT_ALLOWED"),
    BOOKING_DEADLINE_EXCEEDED("예약 마감 시간이 지났습니다.", 400, "SCHEDULE_BOOKING_DEADLINE_EXCEEDED"),
    CAPACITY_EXCEEDED("수업 정원이 초과되었습니다.", 400, "SCHEDULE_CAPACITY_EXCEEDED"),
    ACCESS_DENIED("본인의 수업만 취소할 수 있습니다.",  400 , "SCHEDULE_ACCESS_DENIED" ),

    // Cancellation Validation (Policy)
    CANCELLATION_DEADLINE_EXCEEDED("취소 마감 시간이 지났습니다. 강제 진행 여부를 확인해주세요.", 400, "SCHEDULE_CANCELLATION_DEADLINE_EXCEEDED"),

    // Mirroring
    CANNOT_CHANGE_PAST_SCHEDULE("이미 시작되었거나 지난 수업의 상태는 변경할 수 없습니다.", 400, "SCHEDULE_PAST_IMMUTABLE"),
    SCHEDULE_CONFLICT("해당 시간에 이미 수업 일정이 존재합니다.", 409, "TIMEOFF_SCHEDULE_CONFLICT");

    private final String message;
    private final int statusCode;
    private final String errorCode;
}
