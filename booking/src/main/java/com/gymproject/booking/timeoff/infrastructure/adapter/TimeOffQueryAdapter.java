package com.gymproject.booking.timeoff.infrastructure.adapter;

import com.gymproject.booking.timeoff.exception.TimeOffErrorCode;
import com.gymproject.booking.timeoff.exception.TimeOffException;
import com.gymproject.booking.timeoff.infrastructure.persistence.TrainerTimeOffRepository;
import com.gymproject.common.port.booking.TimeOffQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeOffQueryAdapter implements TimeOffQueryPort {

    private final TrainerTimeOffRepository trainerTimeOffRepository;

    @Override
    public void validateNoTimeOffOverlap(Long trainerId, OffsetDateTime startTime, OffsetDateTime endTime) {
        boolean hasTimeOffConflict = trainerTimeOffRepository.existsConflict(trainerId, startTime, endTime);

        if (hasTimeOffConflict) {
            throw new TimeOffException(TimeOffErrorCode.TIME_OFF_CONFLICT);
        }
    }
}
