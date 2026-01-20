package com.gymproject.classmanagement.recurrence.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecurrenceErrorCode {

    // 404: Not Found
    NOT_FOUND("해당 강좌 그룹을 찾을 수 없습니다.", 404, "RECURRENCE_NOT_FOUND"),

    // 400: Business Logic
    INVALID_STATUS("현재 강좌 상태에서는 해당 작업을 수행할 수 없습니다.", 400, "RECURRENCE_INVALID_STATUS"),
    ALREADY_CLOSED_OR_FINISHED("이미 종료되거나 취소된 강좌입니다.", 400, "RECURRENCE_ALREADY_CLOSED"),

    // Create Validation
    INVALID_PERIOD("수업 시작일은 종료일보다 빨라야 합니다.", 400, "RECURRENCE_INVALID_PERIOD"),
    INVALID_DAYS("반복 요일은 최소 하루 이상 선택해야 합니다.", 400, "RECURRENCE_INVALID_DAYS"),
    EXCEED_MAX_PERIOD("기간제 수업은 최대 6개월까지만 설정 가능합니다.", 400, "RECURRENCE_EXCEED_MAX_PERIOD"),

    // Reservation Validation
    ROUTINE_RESERVATION_NOT_ALLOWED("루틴형 수업은 강좌 전체 예약을 지원하지 않습니다.", 400, "RECURRENCE_ROUTINE_NOT_ALLOWED"),
    ALREADY_STARTED("이미 시작된 커리큘럼은 중도 신청/취소가 불가능합니다.", 400, "RECURRENCE_ALREADY_STARTED"),
    CAPACITY_EXCEEDED("커리큘럼 정원이 초과되었습니다.", 400, "RECURRENCE_CAPACITY_EXCEEDED"),

    // Cancellation Validation
    CANCEL_CAPACITY_ERROR("취소 가능한 예약 인원을 초과했습니다. (복구 불가)", 400, "RECURRENCE_CANCEL_CAPACITY_ERROR"),
    ACCESS_DENIED("본인의 강좌만 취소할 수 있습니다.",  400 , "RECURRENCE_ACCESS_DENIED" ),;

    private final String message;
    private final int statusCode;
    private final String errorCode;
}
