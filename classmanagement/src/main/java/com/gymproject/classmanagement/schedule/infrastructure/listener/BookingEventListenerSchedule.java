package com.gymproject.classmanagement.schedule.infrastructure.listener;

import com.gymproject.classmanagement.schedule.domain.entity.Schedule;
import com.gymproject.classmanagement.schedule.exception.ScheduleErrorCode;
import com.gymproject.classmanagement.schedule.exception.ScheduleException;
import com.gymproject.classmanagement.schedule.infrastructure.persistence.ScheduleRepository;
import com.gymproject.common.event.domain.BookingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventListenerSchedule {

    private final ScheduleRepository scheduleRepository;

    /*
        중요

        BookingService: 트랜잭션 시작 -> 예약 저장 -> 커밋 완료
        BookingEVentTranslator: Affte_commit 후 실행, -> BookingEvent 발행
        BookingEVentListenerSchedule: AFTER_COMMIt 을 기다림..(이러면 이벤트 실행안됨)
        따라서 @EventListener 사용해야함
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(BookingEvent event) {

        log.info("Received BookingEvent {}", event);
        /*
            [중요] 해당 리스너는 1:1 PT에만 해당함.
         */
        if(!event.isPersonalBooking()){
            return;
        }

        // APPROVE, REJECT, CANCEL, ATTEND, NOSHOW, << 이렇게 있음
        String type = event.eventType();
        Schedule schedule = getSchedule(event.scheduleId());

        switch (type) {
            // 예약을 "REJECT" 했으면 수업을 "CANCELLED"로 바뀜
            case "REJECT" -> schedule.cancel(true, 0); // 트레이너 거절
            case"CANCEL"-> schedule.cancel(false,0);
//            case "APPROVE" -> schedule.closePersonal(); -> SCHEDUEL에서는 RESERVED 상태로 냅둬야함.
            case "ATTEND" -> schedule.finish();
            case "NOSHOW" -> schedule.finish();
        }


        // SCHEDULE은 다음과 같음
        // OPEN, CLOSED, CANCELLED, RESERVED, FINISHED

    }
    private Schedule getSchedule(Long scheduleId){
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(()->new ScheduleException(ScheduleErrorCode.NOT_FOUND));
    }


}
