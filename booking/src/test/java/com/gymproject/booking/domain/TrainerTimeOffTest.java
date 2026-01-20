package com.gymproject.booking.domain;

import com.gymproject.booking.timeoff.domain.entity.TrainerTimeOff;
import com.gymproject.booking.timeoff.domain.event.TimeOffChangedEvent;
import com.gymproject.booking.timeoff.domain.type.TimeOffStatus;
import com.gymproject.booking.timeoff.domain.type.TimeOffType;
import com.gymproject.booking.timeoff.exception.TimeOffException;
import domain.util.DomainEventsTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainerTimeOffTest {
    private static final Long USER_ID = 100L;
    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final OffsetDateTime END = NOW.plusHours(2);
    private static final String REASON = "개인 사정";

    @Test
    @DisplayName("휴무 생성 테스트: 상태는 REGISTERED이고 Created 이벤트가 발행된다.")
    void create() {
        // when
        TrainerTimeOff timeOff = TrainerTimeOff.create(
                USER_ID, NOW, END, TimeOffType.PERSONAL, REASON
        );

        // then
        assertThat(timeOff.getStatus()).isEqualTo(TimeOffStatus.REGISTERED);
        assertThat(timeOff.getType()).isEqualTo(TimeOffType.PERSONAL);
        assertThat(timeOff.getReason()).isEqualTo(REASON);

        // 이벤트 검증
        List<Object> events = DomainEventsTestUtils.getEvents(timeOff);
        assertThat(events).hasSize(1);

        TimeOffChangedEvent event = (TimeOffChangedEvent) events.get(0);
        assertThat(event.getEventType()).isEqualTo(TimeOffChangedEvent.EventType.CREATE);
    }

    @Test
    @DisplayName("휴무 취소 테스트: 정상 취소 시 CANCELLED 상태 및 Cancelled 이벤트 발행")
    void cancel() {
        // given
        TrainerTimeOff timeOff = createTimeOff();
        DomainEventsTestUtils.clearEvents(timeOff); // 생성 이벤트 지우기

        // when
        timeOff.cancelTimeOff(USER_ID);

        // then
        assertThat(timeOff.getStatus()).isEqualTo(TimeOffStatus.CANCELLED);

        // 이벤트 검증
        List<Object> events = DomainEventsTestUtils.getEvents(timeOff);
        assertThat(events).isNotEmpty();

        TimeOffChangedEvent event = (TimeOffChangedEvent) events.get(0);

        assertThat(event).isInstanceOf(TimeOffChangedEvent.class);
        assertThat(event.getTimeOff().getStatus()).isEqualTo(TimeOffStatus.CANCELLED);
    }

    @Test
    @DisplayName("취소 실패: 다른 사람이 취소 시도 (ACCESS_DENIED)")
    void cancel_Fail_Owner() {
        // given
        TrainerTimeOff timeOff = createTimeOff();
        Long otherUserId = 999L;

        // when & then
        assertThatThrownBy(() -> timeOff.cancelTimeOff(otherUserId))
                .isInstanceOf(TimeOffException.class)
                .hasFieldOrPropertyWithValue("errorCode", "TIMEOFF_ACCESS_DENIED");
    }

    @Test
    @DisplayName("취소 실패: 이미 취소된 휴무 (ALREADY_CANCELLED)")
    void cancel_Fail_Already() {
        // given
        TrainerTimeOff timeOff = createTimeOff();
        timeOff.cancelTimeOff(USER_ID); // 1차 취소 성공

        // when & then (2차 취소 시도)
        assertThatThrownBy(() -> timeOff.cancelTimeOff(USER_ID))
                .isInstanceOf(TimeOffException.class)
                .hasFieldOrPropertyWithValue("errorCode", "TIMEOFF_ALREADY_CANCELLED");
    }

    // 헬퍼 메서드
    private TrainerTimeOff createTimeOff() {
        return TrainerTimeOff.create(
                USER_ID, NOW, END, TimeOffType.PERSONAL, REASON
        );
    }
}