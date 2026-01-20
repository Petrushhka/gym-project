package com.gymproject.booking.booking.infrastructure.listener;

import com.gymproject.booking.booking.application.BookingService;
import com.gymproject.common.event.domain.ScheduleEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduleCancelListener {

    private final BookingService bookingService;
    public static Logger log = LoggerFactory.getLogger(ScheduleCancelListener.class);

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 또다른 트랜잭션 만들기(히스토리는 저장이 안되어도 됨)
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    /* [중요]
        현재 ScheduleEventTranslator 는 phase = AFTER_COMMIT 안에서 이벤트를 던짐.
        그런데 현재 Listner 역시 AFTER_COMMIT으로 받고 있음.

        AFTER_COMMIT 시점은 원래의 DB 트랜잭션이 완전히 종료된 (Commit)된 상태임.
        그런데 Booking 쪽 리스너가 다시 한번 트랜잭션이 커밋 될 때 (AFTER_COMMIT) 실행해줘라고 요청하면,
        스프링은 이미 커밋이 끝났는데 어떤 트랜잭션의 커밋을 기다리는지 몰라서 무시하게됨.

        따라서 @Transactional(phase = AFTER_COMMIT) -> @EventLister로 변경


        [기존 상황 정리]
        1. ReccurenceGroup -> 이벤트 발생
        2. 이벤트 -> ScheduleListener 받기 성공!
        3. ScheduleEventTranslator 외부이벤트로 던지기! (AFTER_COMMIT 시점: 트랜잭션이 다 끝나고 던지는 이벤트임)
        4. ScheduleCancelListner 이벤트 못받음 X (트랜잭션이 없는데 기다리고 있는 상황)
     */
    public void handle(ScheduleEvent event) {

        // 1. 폐강되었는지 확인
        if (!event.status().equalsIgnoreCase("CANCELLED")) {
            return;
        }

        bookingService.cancelBookingBySchedule(event.scheduleId(), event.trainerId());


    }

}
