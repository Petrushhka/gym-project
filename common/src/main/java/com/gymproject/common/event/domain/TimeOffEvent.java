package com.gymproject.common.event.domain;

import java.time.OffsetDateTime;

public record TimeOffEvent (
    Long timeOffId, // source ID
    Long trainerId, // Trainer ID
    OffsetDateTime startAt,
    OffsetDateTime endAt,
    String eventType,
    String reason
){}
