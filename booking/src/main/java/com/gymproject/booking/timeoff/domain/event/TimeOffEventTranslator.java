package com.gymproject.booking.timeoff.domain.event;

import com.gymproject.booking.timeoff.domain.entity.TrainerTimeOff;
import com.gymproject.common.event.domain.TimeOffEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TimeOffEventTranslator {

    private final ApplicationEventPublisher applicationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void translate(TimeOffChangedEvent internalEvent){

        // TimeOff 정보 가져오기
        TrainerTimeOff timeOffEvent = internalEvent.getTimeOff();

        // 외부 이벤트객체로 매핑
        TimeOffEvent externalEvent =
                new TimeOffEvent(
                        timeOffEvent.getTrainerBlockId(),
                        timeOffEvent.getUserId(),
                        timeOffEvent.getStartAt(),
                        timeOffEvent.getEndAt(),
                        timeOffEvent.getStatus().name(),
                        timeOffEvent.getReason()
                );

        // 이벤트 발행
        applicationEventPublisher.publishEvent(externalEvent);
    }

}
