package com.gymproject.classmanagement.schedule.infrastructure.adapter;

import com.gymproject.classmanagement.recurrence.application.RecurrenceService;
import com.gymproject.classmanagement.schedule.application.ScheduleService;
import com.gymproject.common.port.classmanagement.ScheduleCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
@Component
public class ScheduleCommandAdapter implements ScheduleCommandPort {

    private final ScheduleService scheduleService;
    private final RecurrenceService recurrenceService;

    // 1. 1:1 수업 생성
    @Override
    public Long createSingleSchedule(Long trainerId, OffsetDateTime startAt, OffsetDateTime endAt) {
        return scheduleService.createSingleSchedule(trainerId, startAt, endAt);
    }
    // 2. Curriculum 수업 예약
    @Override
    public void reserveCurriculum(Long recurrenceGroupId) {
        recurrenceService.reserveCurriculum(recurrenceGroupId);
    }
   // 3. Routine 수업 예약
    @Override
    public void reserveRoutine(Long scheduleId) {
        scheduleService.reserveRoutine(scheduleId);
    }

    //4. Routine 수업 예약 취소
    @Override
    public void cancelRoutineReservation(Long scheduleId) {
        scheduleService.cancelRoutineReservation(scheduleId);
    }

    // 5. Curriculum 수업 취소
    @Override
    public void cancelCurriculumReservation(Long recurrenceId) {
        recurrenceService.cancelCurriculumReservation(recurrenceId);
    }


}
