package com.gymproject.booking.timeoff.domain.event;

import com.gymproject.booking.timeoff.domain.entity.TrainerTimeOff;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import static com.gymproject.booking.timeoff.domain.event.TimeOffChangedEvent.EventType.CANCEL;
import static com.gymproject.booking.timeoff.domain.event.TimeOffChangedEvent.EventType.CREATE;

@Getter
@ToString
@RequiredArgsConstructor
public class TimeOffChangedEvent {

    private final TrainerTimeOff timeOff;
    private final EventType eventType;

    public enum EventType {
        CREATE, CANCEL // 휴무 시간 생성, 휴무 시간 취소
    }

    // 1. 생성
    public static TimeOffChangedEvent created(TrainerTimeOff timeOff) {
        return new TimeOffChangedEvent(timeOff, CREATE);
    }

    // 2. 취소
    public static TimeOffChangedEvent cancelled(TrainerTimeOff timeOff) {
        return new TimeOffChangedEvent(timeOff, CANCEL);
    }

}
