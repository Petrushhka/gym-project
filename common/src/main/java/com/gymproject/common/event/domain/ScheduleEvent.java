package com.gymproject.common.event.domain;

import java.time.OffsetDateTime;

public record ScheduleEvent(
        Long scheduleId, // source ID
        Long trainerId, // Trainer ID
        OffsetDateTime startAt,
        OffsetDateTime endAt,

        String status,

        String title,
        Long capacity, // 전체정원
        Long currentBookingCount // 현재 예약된 인원
){}
/*
    [고려]
    String eventType의 경우에는 Enum -> String으로 전달하는거라
    타입 안정성이 떨어짐 자칫하면 에러가 날 수 있는 상황이 발생함.

 */