package com.gymproject.classmanagement.recurrence.domain.policy;

import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceType;
import com.gymproject.classmanagement.recurrence.exception.RecurrenceErrorCode;
import com.gymproject.classmanagement.recurrence.exception.RecurrenceException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

///  [중요]
/// policy : 규칙이 상황에 따라 변하는 로직일 때
///  Enum : 규칙이 변하지 않는 속성일 때
public class RecurrencePolicy {

    private static final int MAX_RECURRENCE_MONTHS = 6; // 최대 6개월까지만 연속수업 개설 가능

    // 1. 생성 시 기초 검증
    public static void validateCreate(LocalDate startDate, LocalDate endDate, List<DayOfWeek> repeatDays) {
        if (startDate.isAfter(endDate)) {
            throw new RecurrenceException(RecurrenceErrorCode.INVALID_PERIOD);
        }
        if (repeatDays == null || repeatDays.isEmpty()) {
            throw new RecurrenceException(RecurrenceErrorCode.INVALID_DAYS);
        }
        // 3. 최대 기간 제한 (너무 길게 생성하는 실수 방지)
        if (startDate.plusMonths(MAX_RECURRENCE_MONTHS).isBefore(endDate)) {
            throw new RecurrenceException(RecurrenceErrorCode.EXCEED_MAX_PERIOD);
        }

    }

    // 2. 커리큘럼(PROGRAM)형 예약 가능 여부 검증
    public static void validateProgramReservation(RecurrenceType type, LocalDate startDate) {
        // 루틴형은 그룹 단위 예약을 허용하지 않음
        if (!type.isRequireBatching()) {
            throw new RecurrenceException(RecurrenceErrorCode.ROUTINE_RESERVATION_NOT_ALLOWED);
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new RecurrenceException(RecurrenceErrorCode.ALREADY_STARTED);
        }
    }

    // 3. 취소 가능 여부 검증
    public static void validateCancellation(LocalDate startDate) {
        if (startDate.isBefore(LocalDate.now())) {
            throw new RecurrenceException(RecurrenceErrorCode.ALREADY_STARTED);
        }
    }

}
