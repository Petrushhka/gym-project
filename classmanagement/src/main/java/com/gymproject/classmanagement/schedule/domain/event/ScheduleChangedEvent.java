package com.gymproject.classmanagement.schedule.domain.event;

import com.gymproject.classmanagement.schedule.domain.entity.Schedule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ScheduleChangedEvent {
    private final Schedule schedule;
    private final EventType eventType;

    public enum EventType {
        CREATED, // 생성됨
        UPDATED, // 예약, 취소 등으로 정보 변경
        CANCELLED, // 폐강, 취소
        FINISHED // 종료
    }

    public static ScheduleChangedEvent created(Schedule schedule) {
        return new ScheduleChangedEvent(schedule, EventType.CREATED);
    }

    public static ScheduleChangedEvent cancelled(Schedule schedule) {
        return new ScheduleChangedEvent(schedule, EventType.CANCELLED);
    }

    public static ScheduleChangedEvent updated(Schedule schedule) {
        return new ScheduleChangedEvent(schedule, EventType.UPDATED);
    }

    public static ScheduleChangedEvent finished(Schedule schedule) {
        return new ScheduleChangedEvent(schedule, EventType.FINISHED);
    }
}
