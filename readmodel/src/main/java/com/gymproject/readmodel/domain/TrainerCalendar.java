package com.gymproject.readmodel.domain;

import com.gymproject.common.event.domain.ScheduleEvent;
import com.gymproject.common.event.domain.TimeOffEvent;
import com.gymproject.readmodel.domain.type.CalendarSource;
import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import io.hypersistence.utils.hibernate.type.range.Range;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.ZonedDateTime;

@Entity
@Getter
@Table(name = "TRAINER_CALENDAR_R")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainerCalendar extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calendar_id", nullable = false)
    private Long calendarId;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;


    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @Column(name = "trainer_name", nullable = false)
    private String trainerName;

    @Column(name = "title", nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private CalendarSource sourceType;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "time_range", columnDefinition = "tstzrange")
    private Range<ZonedDateTime> timeRange; // 3rd party 라이브러리가 zonedDateTime만 지원해줌.

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private CalendarStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private TrainerCalendar(
            Long trainerId,
            String trainerName,
            CalendarSource sourceType,
            Long sourceId,
            CalendarStatus status,
            Range<ZonedDateTime> timeRange,
            String title
    ) {
        this.trainerId = trainerId;
        this.trainerName = trainerName;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.status = status;
        this.timeRange = timeRange;
        this.title = title;
    }
//
//    // CREATE 이벤트 전용 팩토리(예약/PT)
//    public static TrainerCalendar createBookingEvent(
//            BookingEvent event,
//            String trainerName,
//            String title,
//            Range<ZonedDateTime> timeRange,
//            CalendarStatus status
//    ) {
//        return TrainerCalendar.builder()
//                .trainerId(event.trainerId())
//                .trainerName(trainerName)
//                .title(title)
//                .sourceType(CalendarSource.BOOKING)
//                .sourceId(event.bookingId())
//                .timeRange(timeRange)
//                .status(status)
//                .capacity(1L) // 개인PT는 1명
//                .bookingCount(1L) // 생성과 동시에 1명
//                .isOpen(false) // 바로 닫혀있음
//                .build();
//    }

    // 1] 스케쥴 이벤트
    public static TrainerCalendar createScheduleEvent(
            ScheduleEvent event,
            String trainerName,
            Range<ZonedDateTime> timeRange,
            CalendarStatus status
    ) {
        return TrainerCalendar.builder()
                // TimeOffEvent에 정의된 trainerId(또는 userId)를 넣습니다.
                .trainerId(event.trainerId())
                .trainerName(trainerName)
                .title(event.title()) // 이벤트에 타이틀 추가
                .sourceType(CalendarSource.SCHEDULE)
                .sourceId(event.scheduleId())
                .timeRange(timeRange)
                .status(status)
                .build();
    }


    // 2] 트레이너 휴무 이벤트
    public static TrainerCalendar createTimeOffEvent(
            TimeOffEvent event,
            String trainerName,
            Range<ZonedDateTime> timeRange,
            CalendarStatus status
    ) {
        return TrainerCalendar.builder()
                // TimeOffEvent에 정의된 trainerId(또는 userId)를 넣습니다.
                .trainerId(event.trainerId())
                .trainerName(trainerName)
                .title(event.reason()) // 휴가 사유가 이벤트객체에 있도록 수정
                .sourceType(CalendarSource.BLOCK) // Enum에 TIME_OFF 추가 필수
                .sourceId(event.timeOffId())
                .timeRange(timeRange)
                .status(status)
                .build();
    }

    public void updateInfo(String title, CalendarStatus status){
        this.title = title;
        this.status = status;
    }

    public void updateInfo(CalendarStatus newStatus) {
        this.status = newStatus;
    }

    public void updateInfo(ScheduleEvent event) {
        this.status = CalendarStatus.mapStatus(event);
        }

}

/*
    TRAINER의 모든 정보
    Booking , TrainerTimeOff, Schedule 를 조인하지 않고 한번에 조회하기위한 테이블임
 */

/*
   Read Model의 경우에는 단순히 시간,상태 스냅샷 기록이므로 비교 의미가 없으므로
   equals/hashCode 를 오버라이드 하지 않음.
 */