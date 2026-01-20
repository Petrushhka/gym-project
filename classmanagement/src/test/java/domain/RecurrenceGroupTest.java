package domain;

import com.gymproject.classmanagement.recurrence.domain.entity.RecurrenceGroup;
import com.gymproject.classmanagement.recurrence.domain.event.RecurrenceGroupEvent;
import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceStatus;
import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceType;
import com.gymproject.classmanagement.recurrence.exception.RecurrenceErrorCode;
import com.gymproject.classmanagement.recurrence.exception.RecurrenceException;
import com.gymproject.classmanagement.template.domain.entity.Template;
import domain.util.DomainEventsTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurrenceGroupTest {
    // 테스트용 더미 데이터 생성 헬퍼
    private RecurrenceGroup createRecurrenceGroup(int capacity, RecurrenceType type, LocalDate startDate) {
        // Template은 Mock 처리 (정원만 알려주면 됨)
        Template mockTemplate = Mockito.mock(Template.class);
        Mockito.when(mockTemplate.getCapacity()).thenReturn(capacity);

        return RecurrenceGroup.create(
                1L,
                mockTemplate,
                startDate,
                startDate.plusMonths(1),
                LocalTime.of(10, 0),
                List.of(DayOfWeek.MONDAY),
                "Asia/Seoul",
                type
        );
    }

    @Test
    @DisplayName("성공: 강좌 그룹 생성 시 초기 상태는 OPEN이고 정원이 설정된다")
    void create_success() {
        // given
        Template mockTemplate = Mockito.mock(Template.class);
        Mockito.when(mockTemplate.getCapacity()).thenReturn(20);
        LocalDate start = LocalDate.now().plusDays(1);

        // when
        RecurrenceGroup group = RecurrenceGroup.create(
                1L, mockTemplate, start, start.plusMonths(1),
                LocalTime.of(10, 0), List.of(DayOfWeek.MONDAY), "Asia/Seoul",
                RecurrenceType.CURRICULUM
        );

        // then
        assertThat(group.getRecurrenceStatus()).isEqualTo(RecurrenceStatus.OPEN);
        assertThat(group.getRemainingCapacity()).isEqualTo(20);
        assertThat(group.getRecurrenceType()).isEqualTo(RecurrenceType.CURRICULUM);
    }

    @Test
    @DisplayName("성공: 커리큘럼 예약 시 잔여 좌석이 감소한다")
    void reserve_success() {
        // given (정원 10명, 내일 시작)
        RecurrenceGroup group = createRecurrenceGroup(10, RecurrenceType.CURRICULUM, LocalDate.now().plusDays(1));

        // when
        group.reserveCurriculum();

        // then
        assertThat(group.getRemainingCapacity()).isEqualTo(9);
        assertThat(group.getRecurrenceStatus()).isEqualTo(RecurrenceStatus.OPEN);
    }

    @Test
    @DisplayName("성공: 마지막 좌석 예약 시 상태가 CLOSED로 변경된다")
    void reserve_last_seat() {
        // given (정원 1명)
        RecurrenceGroup group = createRecurrenceGroup(1, RecurrenceType.CURRICULUM, LocalDate.now().plusDays(1));

        // when
        group.reserveCurriculum();

        // then
        assertThat(group.getRemainingCapacity()).isEqualTo(0);
        assertThat(group.getRecurrenceStatus()).isEqualTo(RecurrenceStatus.CLOSED);
    }

    @Test
    @DisplayName("실패: 루틴형 수업은 예약할 수 없다")
    void reserve_fail_routine_type() {
        // given (루틴형)
        RecurrenceGroup group = createRecurrenceGroup(10, RecurrenceType.ROUTINE, LocalDate.now().plusDays(1));

        // when & then
        assertThatThrownBy(group::reserveCurriculum)
                .isInstanceOf(RecurrenceException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecurrenceErrorCode.ROUTINE_RESERVATION_NOT_ALLOWED.getErrorCode());
    }

    @Test
    @DisplayName("성공: 예약 취소 시 좌석이 복구되고 CLOSED 상태가 OPEN으로 변경된다")
    void cancel_reservation_restore() {
        // given (정원 1명인 수업을 예약해서 0명/CLOSED 상태로 만듦)
        RecurrenceGroup group = createRecurrenceGroup(1, RecurrenceType.CURRICULUM, LocalDate.now().plusDays(1));
        group.reserveCurriculum(); // 0, CLOSED

        // when
        group.cancelCurriculumReservation();

        // then
        assertThat(group.getRemainingCapacity()).isEqualTo(1);
        assertThat(group.getRecurrenceStatus()).isEqualTo(RecurrenceStatus.OPEN);
    }

    @Test
    @DisplayName("실패: 정원이 꽉 찬 상태(초기상태)에서 예약을 취소하면 예외 발생 (오버플로우 방지)")
    void cancel_reservation_fail_overflow() {
        // given (정원 10명, 현재 10명 남음)
        RecurrenceGroup group = createRecurrenceGroup(10, RecurrenceType.CURRICULUM, LocalDate.now().plusDays(1));

        // when & then
        assertThatThrownBy(group::cancelCurriculumReservation)
                .isInstanceOf(RecurrenceException.class)
                .hasFieldOrPropertyWithValue("errorCode", RecurrenceErrorCode.CANCEL_CAPACITY_ERROR.getErrorCode());
    }

    @Test
    @DisplayName("성공: 트레이너가 강좌 폐강 시 CANCELLED 상태가 되고 이벤트가 발행된다")
    void cancel_class_by_trainer() {
        // given
        RecurrenceGroup group = createRecurrenceGroup(10, RecurrenceType.CURRICULUM, LocalDate.now().plusDays(1));
        DomainEventsTestUtils.clearEvents(group);
        // when
        group.cancelRecurrenceClass();

        // then
        assertThat(group.getRecurrenceStatus()).isEqualTo(RecurrenceStatus.CANCELLED);

        // 이벤트 검증 (AbstractAggregateRoot 기능)
        List<Object> events = DomainEventsTestUtils.getEvents(group);

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(RecurrenceGroupEvent.class);
    }

}