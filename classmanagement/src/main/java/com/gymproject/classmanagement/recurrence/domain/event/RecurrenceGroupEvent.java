package com.gymproject.classmanagement.recurrence.domain.event;

import com.gymproject.classmanagement.recurrence.domain.entity.RecurrenceGroup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@RequiredArgsConstructor
public class RecurrenceGroupEvent {
    private final RecurrenceGroup recurrenceGroup;
    private final EventType eventType;

    public static Logger log = LoggerFactory.getLogger(RecurrenceGroupEvent.class);

    public enum EventType {
        CREATED, UPDATED, CANCELLED, FINISHED
    }

    // 팩토리 메서드
    public static RecurrenceGroupEvent created(RecurrenceGroup group) {
        return new RecurrenceGroupEvent(group, EventType.CREATED);
    }

    public static RecurrenceGroupEvent updated(RecurrenceGroup group) {
        return new RecurrenceGroupEvent(group, EventType.UPDATED);
    }

    public static RecurrenceGroupEvent cancelled(RecurrenceGroup group) {
        return new RecurrenceGroupEvent(group, EventType.CANCELLED);
    }

    public static RecurrenceGroupEvent finished(RecurrenceGroup group) {
        return new RecurrenceGroupEvent(group, EventType.FINISHED);
    }
}

/*
    기존에는 Builder로 직접 행위를 만들었다면 현재는 정적 팩토리로 해결
 */