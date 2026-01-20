package domain.policy;

import com.gymproject.classmanagement.recurrence.domain.policy.RecurrencePolicy;
import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceType;
import com.gymproject.classmanagement.recurrence.exception.RecurrenceErrorCode;
import com.gymproject.classmanagement.recurrence.exception.RecurrenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurrencePolicyTest {

    @Test
    @DisplayName("생성 정책: 시작일이 종료일보다 늦으면 예외 발생")
    void validateCreate_invalid_period() {
        // given
        LocalDate start = LocalDate.of(2025, 1, 10);
        LocalDate end = LocalDate.of(2025, 1, 1); // 종료일이 더 빠름

        // when & then
        assertThatThrownBy(() ->
                RecurrencePolicy.validateCreate(start, end, List.of(DayOfWeek.MONDAY)))
                .isInstanceOf(RecurrenceException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecurrenceErrorCode.INVALID_PERIOD.getErrorCode());
    }

    @Test
    @DisplayName("생성 정책: 최대 기간(6개월)을 초과하면 예외 발생")
    void validateCreate_exceed_max_period() {
        // given
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 7, 2); // 6개월 + 1일

        // when & then
        assertThatThrownBy(() ->
                RecurrencePolicy.validateCreate(start, end, List.of(DayOfWeek.MONDAY)))
                .isInstanceOf(RecurrenceException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecurrenceErrorCode.EXCEED_MAX_PERIOD.getErrorCode());
    }

    @Test
    @DisplayName("예약 정책: 루틴형(ROUTINE) 수업은 통예약 불가")
    void validateReservation_routine_type() {
        // when & then
        assertThatThrownBy(() ->
                RecurrencePolicy.validateProgramReservation(RecurrenceType.ROUTINE, LocalDate.now().plusDays(1)))
                .isInstanceOf(RecurrenceException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecurrenceErrorCode.ROUTINE_RESERVATION_NOT_ALLOWED.getErrorCode());
    }

    @Test
    @DisplayName("예약 정책: 이미 시작된 수업은 예약 불가")
    void validateReservation_already_started() {
        // given
        LocalDate pastDate = LocalDate.now().minusDays(1); // 어제 시작함

        // when & then
        assertThatThrownBy(() ->
                RecurrencePolicy.validateProgramReservation(RecurrenceType.CURRICULUM, pastDate))
                .isInstanceOf(RecurrenceException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecurrenceErrorCode.ALREADY_STARTED.getErrorCode());
    }

}