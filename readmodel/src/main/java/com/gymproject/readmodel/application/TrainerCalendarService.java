package com.gymproject.readmodel.application;

import com.gymproject.common.event.domain.ScheduleEvent;
import com.gymproject.common.event.domain.TimeOffEvent;
import com.gymproject.common.port.user.UserProfilePort;
import com.gymproject.readmodel.domain.CalendarStatus;
import com.gymproject.readmodel.domain.TrainerCalendar;
import com.gymproject.readmodel.domain.type.CalendarSource;
import com.gymproject.readmodel.infrastructure.persistence.TrainerCalendarRepository;
import io.hypersistence.utils.hibernate.type.range.Range;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import static com.gymproject.common.constant.GymTimePolicy.SERVICE_ZONE;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrainerCalendarService {

    private final TrainerCalendarRepository trainerCalendarRepository;
    private final UserProfilePort userProfilePort;

    @Transactional
    public void synchronizeSchedule(ScheduleEvent event) {
        // 1. 데이터 변환 (Event -> Domain Value)
        Range<ZonedDateTime> range = toRange(event.startAt(), event.endAt());

        // 이벤트로 타입으로부터 달력 상태 매핑
        CalendarStatus newStatus = CalendarStatus.mapStatus(event);

        if (newStatus == null) {
            log.warn("⚠️ 매핑된 CalendarStatus가 null입니다. 이벤트를 무시합니다. ID: {}", event.scheduleId());
            return;
        }

        // 2. 조회 (기존 데이터가 있는지?)
        TrainerCalendar existing = trainerCalendarRepository.findBySourceTypeAndSourceId(
                CalendarSource.SCHEDULE,
                event.scheduleId()
        ).orElse(null);

        // 3. 로직 분기 (생성 vs 수정)
        if (existing == null) {
            createSchedule(event, range, newStatus);
        } else {
            existing.updateInfo(event.title(), newStatus);
            // Dirty Checking으로 자동 저장됨 (명시적 save 생략 가능)
        }
    }

    @Transactional
    public void synchronizeTimeOff(TimeOffEvent event) {
        Range<ZonedDateTime> range = toRange(event.startAt(), event.endAt());
        // 이벤트로 타입으로부터 달력 상태 매핑
        CalendarStatus newStatus = CalendarStatus.mapStatus(event);

        if (newStatus == null) {
            return;
        }

        TrainerCalendar existing = trainerCalendarRepository.findBySourceTypeAndSourceId(
                CalendarSource.BLOCK,
                event.timeOffId()
        ).orElse(null);

        if (existing == null) {
            createTimeOff(event, range, newStatus);
        } else {
            existing.updateInfo(event.reason(), newStatus);
        }
    }

    @Transactional(readOnly = true)
    public List<TrainerCalendarResponse> getCalendar(Long trainerId,
                                                     OffsetDateTime start,
                                                     OffsetDateTime end){

        return trainerCalendarRepository.findAllByTrainerIdAndPeriod(trainerId, start, end)
                .stream()
                .map(TrainerCalendarResponse::create)
                .toList();
    }


    // --- Private Helpers ---

    private void createSchedule(ScheduleEvent event, Range<ZonedDateTime> range, CalendarStatus status) {
        // 외부 포트 호출 (이름 조회)
        String trainerName = userProfilePort.getUserFullName(event.trainerId());

        TrainerCalendar calendar = TrainerCalendar.createScheduleEvent(
                event,
                trainerName,
                range,
                status
        );
        trainerCalendarRepository.save(calendar);
    }

    private void createTimeOff(TimeOffEvent event, Range<ZonedDateTime> range, CalendarStatus status) {
        String trainerName = userProfilePort.getUserFullName(event.trainerId());

        TrainerCalendar calendar = TrainerCalendar.createTimeOffEvent(
                event,
                trainerName,
                range,
                status
        );
        trainerCalendarRepository.save(calendar);
    }

    private Range<ZonedDateTime> toRange(java.time.OffsetDateTime start, java.time.OffsetDateTime end) {
        ZonedDateTime zonedStart = start.atZoneSameInstant(SERVICE_ZONE);
        ZonedDateTime zonedEnd = end.atZoneSameInstant(SERVICE_ZONE);
        return Range.closedOpen(zonedStart, zonedEnd);
    }

}
