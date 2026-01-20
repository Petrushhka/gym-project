package com.gymproject.common.port.booking;

import java.time.OffsetDateTime;

public interface TimeOffQueryPort {
    void validateNoTimeOffOverlap(Long trainerId, OffsetDateTime startTime, OffsetDateTime endTime);

}
