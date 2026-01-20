package com.gymproject.booking.booking.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookingErrorCode {
    // 404: Not Found
    NOT_FOUND("해당 예약 정보를 찾을 수 없습니다.", 404, "BOOKING_NOT_FOUND"),

    // 400: State / Lifecycle Validation (상태 위반)
    INVALID_STATUS("현재 예약 상태에서는 해당 작업을 수행할 수 없습니다.", 400, "BOOKING_INVALID_STATUS"),
    ALREADY_CONFIRMED("이미 확정된 예약입니다.", 400, "BOOKING_ALREADY_CONFIRMED"),
    ALREADY_CANCELLED("이미 취소된 예약입니다.", 400, "BOOKING_ALREADY_CANCELLED"),
    NOT_PENDING_STATUS("승인 대기(PENDING) 상태의 예약만 처리가 가능합니다.", 400, "BOOKING_NOT_PENDING"),
    NOT_CONFIRMED_STATUS("예약 확정(CONFIRMED) 상태에서만 출석/노쇼 처리가 가능합니다.", 400, "BOOKING_NOT_CONFIRMED"),

    // 400: Policy Validation (정책 위반 - 시간/거리/권한)
    BOOKING_DEADLINE_EXCEEDED("예약 마감 시간이 지났습니다.", 400, "BOOKING_DEADLINE_EXCEEDED"),
    CANCELLATION_NOT_ALLOWED("취소 가능한 시간이 지났습니다.", 400, "BOOKING_CANCELLATION_NOT_ALLOWED"),

    CHECK_IN_TOO_EARLY("출석 체크는 수업 시작 15분 전부터 가능합니다.", 400, "BOOKING_CHECK_IN_TOO_EARLY"),
    CHECK_IN_DISTANCE_EXCEEDED("센터와의 거리가 너무 멀어 출석할 수 없습니다.", 400, "BOOKING_CHECK_IN_DISTANCE_EXCEEDED"),

    TICKET_TYPE_NOT_MATCH("해당 이용권으로는 이 수업을 예약할 수 없습니다.", 400, "BOOKING_TICKET_TYPE_MISMATCH"),

    // 409: Conflict (중복/동시성)
    ALREADY_BOOKED_SCHEDULE("이미 해당 수업에 예약이 존재합니다.", 409, "BOOKING_ALREADY_EXISTS"),
    ALREADY_BOOKED_CURRICULUM("이미 해당 커리큘럼(과정)에 참여 중입니다.", 409, "BOOKING_CURRICULUM_ALREADY_JOINED"),
    SESSION_CONCURRENCY_ERROR("동시 요청으로 인해 세션 차감에 실패했습니다. 잠시 후 다시 시도해주세요.", 409, "BOOKING_SESSION_CONCURRENCY_ERROR"),

    // 403: Security / Access
    ACCESS_DENIED("해당 예약에 대한 접근 권한이 없습니다.", 403, "BOOKING_ACCESS_DENIED"),;
    private final String message;
    private final int statusCode;
    private final String errorCode;
}
