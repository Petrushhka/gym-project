package domain.policy;

import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceType;
import com.gymproject.classmanagement.schedule.domain.policy.SchedulePolicy;
import com.gymproject.classmanagement.schedule.exception.ScheduleErrorCode;
import com.gymproject.classmanagement.schedule.exception.ScheduleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchedulePolicyTest {
    @Test
    @DisplayName("예약 정책: 커리큘럼(과정)형 수업은 개별 회차 예약(decreaseCount) 불가")
    void validateReservation_curriculum_fail() {
        // given
        OffsetDateTime future = OffsetDateTime.now().plusDays(1);
        int deadlineHours = 1;

        // when & then
        assertThatThrownBy(() ->
                SchedulePolicy.validateReservation(RecurrenceType.CURRICULUM, future, deadlineHours))
                .isInstanceOf(ScheduleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ScheduleErrorCode.RESERVATION_NOT_ALLOWED_CURRICULUM.getErrorCode());
    }

    @Test
    @DisplayName("예약 정책: 마감 시간이 지나면 예약 불가")
    void validateReservation_deadline_exceeded() {
        // given
        // 수업 시작 30분 전 (마감은 1시간 전이라고 가정) -> 이미 마감됨
        OffsetDateTime startAt = OffsetDateTime.now().plusMinutes(30);
        int deadlineHours = 1;

        // when & then
        assertThatThrownBy(() ->
                SchedulePolicy.validateReservation(RecurrenceType.ROUTINE, startAt, deadlineHours))
                .isInstanceOf(ScheduleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ScheduleErrorCode.BOOKING_DEADLINE_EXCEEDED.getErrorCode());
    }

    @Test
    @DisplayName("취소 정책: 마감 시간이 지났는데 강제(Force)가 아니면 취소 불가")
    void validateCancellationByTrainer_deadline_fail() {
        // given
        OffsetDateTime startAt = OffsetDateTime.now().plusMinutes(30); // 30분 뒤 시작 (1시간 전 마감 정책 위반)
        int deadlineHours = 1;
        boolean isForce = false; // 강제 아님

        // when & then
        assertThatThrownBy(() ->
                SchedulePolicy.validateCancellationByTrainer(startAt, deadlineHours, isForce))
                .isInstanceOf(ScheduleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ScheduleErrorCode.CANCELLATION_DEADLINE_EXCEEDED.getErrorCode());
    }

    @Test
    @DisplayName("취소 정책: 마감 시간이 지났어도 강제(Force)면 취소 가능 (예외 안 터짐)")
    void validateCancellationByTrainer_force_success() {
        // given
        OffsetDateTime startAt = OffsetDateTime.now().plusMinutes(30);
        int deadlineHours = 1;
        boolean isForce = true; // 강제 취소

        // when & then (No Exception)
        SchedulePolicy.validateCancellationByTrainer(startAt, deadlineHours, isForce);
    }

    @Test
    @DisplayName("미러링 정책: 이미 시작된(과거의) 수업 상태는 변경 불가")
    void validateMirroring_past_fail() {
        // given
        OffsetDateTime past = OffsetDateTime.now().minusHours(1);

        // when & then
        assertThatThrownBy(() -> SchedulePolicy.validateMirroring(past))
                .isInstanceOf(ScheduleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ScheduleErrorCode.CANNOT_CHANGE_PAST_SCHEDULE.getErrorCode());
    }
}