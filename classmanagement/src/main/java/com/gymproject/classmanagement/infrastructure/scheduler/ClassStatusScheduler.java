package com.gymproject.classmanagement.infrastructure.scheduler;

import com.gymproject.classmanagement.recurrence.domain.entity.RecurrenceGroup;
import com.gymproject.classmanagement.schedule.domain.entity.Schedule;
import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceStatus;
import com.gymproject.classmanagement.schedule.domain.type.ScheduleStatus;
import com.gymproject.classmanagement.recurrence.infrastructure.persistence.RecurrenceGroupRepository;
import com.gymproject.classmanagement.schedule.infrastructure.persistence.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ClassStatusScheduler {

    private final ScheduleRepository scheduleRepository;
    private final RecurrenceGroupRepository recurrenceGroupRepository;

    // 매 10분 마다 실행
    @Scheduled(cron = "0 0/10 * * * *")
    @Transactional
    public void autoFinish(){

        // 1. 개별 수업 종료 처리
        autoFinishSchedules();

        // 2. 기간제 수업 종료
        autoFinishRecurrences();
    }

    // 개별 수업 종료(Schedule 단위)
    private void autoFinishSchedules(){

        OffsetDateTime now = OffsetDateTime.now();
        List<String> statuses = List.of(ScheduleStatus.OPEN.name(), ScheduleStatus.RESERVED.name(), ScheduleStatus.CLOSED.name());

        //1. 상태가 OPEN/CLOSED/RESERVED 이면서 종료 시간이 지난 수업 조회
        List<Schedule> schedules = scheduleRepository.findExpiredClasses(statuses, now.toLocalDate());

        // 2. finish 메서드 호출
        for(Schedule schedule : schedules){
            schedule.finish();
//            scheduleRepository.save(schedule);
        }

        // 3. 저장 및 이벤트 호출
        scheduleRepository.saveAll(schedules);
    }

    // 그룹 수업 전체 종료(Recurrence 단위)
    private void autoFinishRecurrences(){

        OffsetDateTime now = OffsetDateTime.now();
        List<String> statuses = List.of(RecurrenceStatus.OPEN.name(), RecurrenceStatus.CLOSED.name());

        List<RecurrenceGroup> recurrenceGroups = recurrenceGroupRepository.findExpiredGroups(statuses, now.toLocalDate());

        for(RecurrenceGroup recurrenceGroup : recurrenceGroups){
            recurrenceGroup.finish();
        }
        recurrenceGroupRepository.saveAll(recurrenceGroups);

    }

}
