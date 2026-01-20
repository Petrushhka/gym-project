package com.gymproject.common.dto.schedule;

import java.time.OffsetDateTime;

public record ScheduleInfo(
        Long scheduleId,
        Long trainerId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String status,
        int capacity,
        String title
) {

}
