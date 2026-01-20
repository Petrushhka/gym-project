package com.gymproject.booking.timeoff.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TimeOffErrorCode {
    // 404: Not Found
    TIME_OFF_NOT_FOUND("해당 휴무 정보를 찾을 수 없습니다.", 404, "TIMEOFF_NOT_FOUND"),

    // 400: Validation
    INVALID_TIME_RANGE("종료 시간은 시작 시간보다 이후여야 합니다.", 400, "TIMEOFF_INVALID_TIME_RANGE"),
    INVALID_START_TIME("시작시간은 과거로 지정할 수 없습니다.",400,  "INVALID_START_TIME"),
    ALREADY_CANCELLED("이미 취소된 휴무입니다.", 400, "TIMEOFF_ALREADY_CANCELLED"),

    // 403: Forbidden
    ACCESS_DENIED("본인의 휴무 일정만 관리할 수 있습니다.", 403, "TIMEOFF_ACCESS_DENIED"),

    // 409: Conflict (중복)
    TIME_OFF_CONFLICT("해당 시간에 휴무가 등록되어 있습니다.", 409, "TIMEOFF_CONFLICT"),
    SCHEDULE_CONFLICT("해당 시간에 이미 수업 일정이 존재합니다.", 409, "TIMEOFF_SCHEDULE_CONFLICT");

    private final String message;
    private final int statusCode;
    private final String errorCode;
}
