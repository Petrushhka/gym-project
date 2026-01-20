package com.gymproject.readmodel.domain;

import com.gymproject.common.event.domain.ScheduleEvent;
import com.gymproject.common.event.domain.TimeOffEvent;
import com.gymproject.readmodel.domain.type.CalendarSource;
import io.hypersistence.utils.hibernate.type.range.Range;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerCalendarTest {

    private static final Long TRAINER_ID = 10L;
    private static final String TRAINER_NAME = "헬스장_홍길동";
    private static final Long SOURCE_ID = 100L;
    private static final OffsetDateTime START = OffsetDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime END = START.plusHours(1);

    // 테스트용 Range 생성 헬퍼
    private Range<ZonedDateTime> createRange() {
        return Range.closedOpen(
                START.toZonedDateTime(),
                END.toZonedDateTime()
        );
    }

    @Test
    @DisplayName("ScheduleEvent로 객체 생성 시 SCHEDULE 타입으로 매핑된다")
    void createScheduleEvent() {
        // given
        String title = "오전 PT 수업";
        Range<ZonedDateTime> range = createRange();
        CalendarStatus status = CalendarStatus.CLASS_OPEN;

        ScheduleEvent event = new ScheduleEvent(
                SOURCE_ID,
                TRAINER_ID,
                START,
                END,
                "OPEN",
                title,
                5L, // capacity (ReadModel엔 저장 안되지만 이벤트엔 있음)
                0L
        );

        // when
        TrainerCalendar calendar = TrainerCalendar.createScheduleEvent(
                event,
                TRAINER_NAME,
                range,
                status
        );

        // then
        assertThat(calendar.getTrainerId()).isEqualTo(TRAINER_ID);
        assertThat(calendar.getTrainerName()).isEqualTo(TRAINER_NAME);
        assertThat(calendar.getSourceType()).isEqualTo(CalendarSource.SCHEDULE); // 핵심
        assertThat(calendar.getSourceId()).isEqualTo(SOURCE_ID);
        assertThat(calendar.getTitle()).isEqualTo(title);
        assertThat(calendar.getStatus()).isEqualTo(status);
        assertThat(calendar.getTimeRange()).isEqualTo(range);
    }

    @Test
    @DisplayName("TimeOffEvent로 객체 생성 시 BLOCK 타입으로 매핑된다")
    void createTimeOffEvent() {
        // given
        String reason = "개인 사정";
        Range<ZonedDateTime> range = createRange();
        CalendarStatus status = CalendarStatus.TIMEOFF_ACTIVE;

        TimeOffEvent event = new TimeOffEvent(
                SOURCE_ID, // timeOffId
                TRAINER_ID,
                START,
                END,
                "REGISTERED",
                reason
        );

        // when
        TrainerCalendar calendar = TrainerCalendar.createTimeOffEvent(
                event,
                TRAINER_NAME,
                range,
                status
        );

        // then
        assertThat(calendar.getTrainerId()).isEqualTo(TRAINER_ID);
        assertThat(calendar.getSourceType()).isEqualTo(CalendarSource.BLOCK); // 핵심
        assertThat(calendar.getSourceId()).isEqualTo(SOURCE_ID);
        assertThat(calendar.getTitle()).isEqualTo(reason); // 사유가 제목으로 들어가는지 확인
        assertThat(calendar.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("정보 업데이트(제목, 상태)가 정상적으로 반영된다")
    void updateInfo_TitleAndStatus() {
        // given
        TrainerCalendar calendar = createDummyCalendar();
        String newTitle = "변경전 제목";
        CalendarStatus newStatus = CalendarStatus.CLASS_CLOSED;

        // when
        calendar.updateInfo(newTitle, newStatus);

        // then
        assertThat(calendar.getTitle()).isEqualTo(newTitle);
        assertThat(calendar.getStatus()).isEqualTo(newStatus);
    }

    @Test
    @DisplayName("상태 업데이트가 정상적으로 반영된다")
    void updateInfo_StatusOnly() {
        // given
        TrainerCalendar calendar = createDummyCalendar();
        CalendarStatus newStatus = CalendarStatus.CLASS_CANCELLED;

        // when
        calendar.updateInfo(newStatus);

        // then
        assertThat(calendar.getStatus()).isEqualTo(newStatus);
    }

    @Test
    @DisplayName("ScheduleEvent를 통한 업데이트 시 상태가 매핑되어 반영된다")
    void updateInfo_ByEvent() {
        // given
        TrainerCalendar calendar = createDummyCalendar();

        // CLOSED 이벤트를 받음
        ScheduleEvent event = new ScheduleEvent(
                SOURCE_ID, TRAINER_ID, START, END,
                "CLOSED", // CalendarStatus.mapStatus("CLOSED") -> CLASS_CLOSED 가정
                "타이틀", 5L, 5L
        );

        // when
        calendar.updateInfo(event);

        // then
        // CalendarStatus.mapStatus 로직에 따라 매핑된 결과 확인
        // (Enum 로직에 따라 다를 수 있으나, 일반적으로 CLASS_CLOSED 예상)
        assertThat(calendar.getStatus()).isEqualTo(CalendarStatus.CLASS_CLOSED);
    }

    // --- Helper ---
    private TrainerCalendar createDummyCalendar() {
        return TrainerCalendar.createScheduleEvent(
                new ScheduleEvent(SOURCE_ID, TRAINER_ID, START, END, "OPEN", "기본", 1L, 0L),
                TRAINER_NAME,
                createRange(),
                CalendarStatus.CLASS_OPEN
        );
    }

}