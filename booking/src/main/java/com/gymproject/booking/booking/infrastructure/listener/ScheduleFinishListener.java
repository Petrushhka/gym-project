package com.gymproject.booking.booking.infrastructure.listener;

import com.gymproject.booking.booking.application.BookingService;
import com.gymproject.common.event.domain.ScheduleEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduleFinishListener {

    private final BookingService bookingService;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(ScheduleEvent event){
        // FINSHED 된 경우에만 처리
        if(!event.status().equalsIgnoreCase("FINISHED")){
            return;
        }

        bookingService.processAutoNoShow(event.scheduleId());

    }
}

/* [중요]
    해당 리스너는 스케줄러에 의해 자동으로 FINISH 되는 수업들에 이벤트를 받아서
    NOSHOW 처리를 진행하는 리스너임.

    기존에는 스케줄러를 Booking 모듈에서도 돌릴 생각이었으나
    Scheudule 스케줄러에서 발행되는 이벤트를 듣고 처리하는 것이 훨씬 효율적임.
 */