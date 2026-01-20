package domain;

import com.gymproject.classmanagement.recurrence.domain.entity.RecurrenceGroup;
import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceStatus;
import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceType;
import com.gymproject.classmanagement.schedule.domain.entity.Schedule;
import com.gymproject.classmanagement.schedule.domain.event.ScheduleChangedEvent;
import com.gymproject.classmanagement.schedule.domain.type.ScheduleStatus;
import com.gymproject.classmanagement.schedule.exception.ScheduleErrorCode;
import com.gymproject.classmanagement.schedule.exception.ScheduleException;
import com.gymproject.classmanagement.template.domain.entity.Template;
import domain.util.DomainEventsTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class ScheduleTest {
    // [Helper] 템플릿 Mock 생성
    private Template createMockTemplate(int capacity) {
        Template mock = Mockito.mock(Template.class);
        when(mock.getCapacity()).thenReturn(capacity);
        return mock;
    }

    // [Helper] RecurrenceGroup Mock 생성
    private RecurrenceGroup createMockGroup(RecurrenceType type) {
        RecurrenceGroup mock = Mockito.mock(RecurrenceGroup.class);
        when(mock.getRecurrenceType()).thenReturn(type);
        when(mock.getTrainerId()).thenReturn(1L);
        return mock;
    }

    @Test
    @DisplayName("성공: 원데이 클래스 생성 시 초기 상태는 OPEN이고, Created 이벤트가 발생한다")
    void createOneTime_success() {
        // given
        Template template = createMockTemplate(10);
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        OffsetDateTime end = start.plusHours(1);

        // when
        Schedule schedule = Schedule.createOneTime(1L, template, start, end);

        // then
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.OPEN);
        assertThat(schedule.getCapacity()).isEqualTo(10);

        // 이벤트 검증
        List<Object> events = DomainEventsTestUtils.getEvents(schedule);
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(ScheduleChangedEvent.class);
    }

    @Test
    @DisplayName("성공: 예약(decreaseCount) 시 정원이 감소하고 Updated 이벤트가 발생한다")
    void decreaseCount_success() {
        // given (정원 10명, 루틴형)
        Template template = createMockTemplate(10);
        RecurrenceGroup group = createMockGroup(RecurrenceType.ROUTINE);
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);

        Schedule schedule = Schedule.createRecurrence(group, template, start, start.plusHours(1));

        // when (1시간 전 마감 기준)
        schedule.decreaseCount(1);

        // then
        assertThat(schedule.getCapacity()).isEqualTo(9); // 10 -> 9
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.OPEN);

        // 이벤트: Created(생성시) + Updated(예약시) = 2개
        List<Object> events = DomainEventsTestUtils.getEvents(schedule);
        assertThat(events).hasSize(2);
    }

    @Test
    @DisplayName("성공: 마지막 한 자리를 예약하면 상태가 CLOSED로 변경된다")
    void decreaseCount_close_when_full() {
        // given (정원 1명)
        Template template = createMockTemplate(1);
        RecurrenceGroup group = createMockGroup(RecurrenceType.ROUTINE);
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);

        Schedule schedule = Schedule.createRecurrence(group, template, start, start.plusHours(1));

        // when
        schedule.decreaseCount(1);

        // then
        assertThat(schedule.getCapacity()).isEqualTo(0);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.CLOSED);
    }

    @Test
    @DisplayName("실패: 정원이 0명이거나 이미 CLOSED 된 상태에서는 예약 불가")
    void decreaseCount_fail_capacity_zero() {
        // given (정원 1명 -> 예약해서 0명 만듦)
        Template template = createMockTemplate(1);
        RecurrenceGroup group = createMockGroup(RecurrenceType.ROUTINE);
        Schedule schedule = Schedule.createRecurrence(group, template, OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(1));
        schedule.decreaseCount(1); // CLOSED 상태됨

        // when & then
        assertThatThrownBy(() -> schedule.decreaseCount(1))
                .isInstanceOf(ScheduleException.class)
                .hasFieldOrPropertyWithValue("errorCode", ScheduleErrorCode.CAPACITY_EXCEEDED.getErrorCode());
    }

    @Test
    @DisplayName("성공: 트레이너가 강제 취소(Force)하면 상태가 CANCELLED로 변경된다")
    void cancel_by_trainer_success() {
        // given
        Schedule schedule = Schedule.createOneTime(1L, createMockTemplate(5), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(1));

        // when (강제 취소 true)
        schedule.cancel(true, 1);

        // then
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
        // 이벤트: Created + Cancelled
        List<Object> events = DomainEventsTestUtils.getEvents(schedule);
        assertThat(events).hasSize(2);
    }

    @Test
    @DisplayName("성공: 미러링 시 부모(Recurrence) 상태와 인원을 따라간다")
    void mirrorParentStatus_success() {
        // given
        RecurrenceGroup group = createMockGroup(RecurrenceType.CURRICULUM);
        Schedule schedule = Schedule.createRecurrence(group, createMockTemplate(10),
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(1));

        // when (부모가 남은자리 5명에 CLOSED 상태라고 전달)
        schedule.mirrorParentStatus(5, RecurrenceStatus.CLOSED);

        // then
        assertThat(schedule.getCapacity()).isEqualTo(5);
        assertThat(schedule.getStatus()).isEqualTo(ScheduleStatus.CLOSED);
    }

}