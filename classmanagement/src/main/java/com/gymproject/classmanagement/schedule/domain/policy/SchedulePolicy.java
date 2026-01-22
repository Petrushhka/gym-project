package com.gymproject.classmanagement.schedule.domain.policy;

import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceType;
import com.gymproject.classmanagement.schedule.exception.ScheduleErrorCode;
import com.gymproject.classmanagement.schedule.exception.ScheduleException;
import com.gymproject.common.util.GymDateUtil;

import java.time.OffsetDateTime;

public class SchedulePolicy {

    /**
     * 개별 수업 예약(수량 감소) 가능 여부 검증
     */
    public static void validateReservation(RecurrenceType type, OffsetDateTime startAt,
                                           int deadlineHours) {

        // 1. 커리큘럼형(PROGRAM) 정책
        if (type == RecurrenceType.CURRICULUM) {
            throw new ScheduleException(ScheduleErrorCode.RESERVATION_NOT_ALLOWED_CURRICULUM);
        }
        // 2. 시간 검증
        if (GymDateUtil.now().isAfter(startAt.minusHours(deadlineHours))) {
            throw new ScheduleException(ScheduleErrorCode.BOOKING_DEADLINE_EXCEEDED, "마감시간: " + deadlineHours + "시간 전");
        }
    }

    /**
     * 트레이너 수업 취소 정책 검증(데드라인, 강제로 취소하는지)
     */
    // 트레이너가 수업 자체를 취소할 때 (Class Closing)
    public static void validateCancellationByTrainer(OffsetDateTime startAt,
                                                     int deadlineHours,
                                                     boolean isForce) { // '강제 진행' 플래그 추가
        // 마감 시간이 지났더라도 트레이너가 '강제(Force)'로 진행하겠다면 허용
        if (!isForce && GymDateUtil.now().isAfter(startAt.minusHours(1))) {
            throw new ScheduleException(ScheduleErrorCode.CANCELLATION_DEADLINE_EXCEEDED);
        }
    }

    /**
     * 커리큘럼 상태 미러링 가능 여부 검증
     */
    public static void validateMirroring(OffsetDateTime startAt) {
        if (startAt.isBefore(GymDateUtil.now())) {
            throw new ScheduleException(ScheduleErrorCode.CANNOT_CHANGE_PAST_SCHEDULE);
        }
    }

    }


