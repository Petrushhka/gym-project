package com.gymproject.classmanagement.schedule.infrastructure.listener;

import com.gymproject.classmanagement.recurrence.domain.entity.RecurrenceGroup;
import com.gymproject.classmanagement.recurrence.domain.event.RecurrenceGroupEvent;
import com.gymproject.classmanagement.schedule.application.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecurrenceGroupListener {

    private final ScheduleService scheduleService;

    @EventListener
    public void handle(RecurrenceGroupEvent event) {
        RecurrenceGroup group = event.getRecurrenceGroup();

        switch (event.getEventType()) {
            // Recurrence 수업은 모두 이걸로 생성
            case CREATED -> scheduleService.createSchedulesFromGroup(group);

            case UPDATED -> scheduleService.syncSchedulesWithGroup(
                    group.getGroupId(),
                    group
            );

            case CANCELLED -> scheduleService.cancelSchedulesByRecurrence(group.getGroupId());
        }

    }
}
