package com.gymproject.readmodel.infrastructure.listener;

import com.gymproject.common.event.domain.TimeOffEvent;
import com.gymproject.readmodel.application.TrainerCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TimeOffEventListener {

    private final TrainerCalendarService trainerCalendarService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @EventListener
    public void handle(TimeOffEvent event) {
        trainerCalendarService.synchronizeTimeOff(event);
    }

}
