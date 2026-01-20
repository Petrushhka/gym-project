package com.gymproject.booking.booking.infrastructure.listener;

import com.gymproject.booking.booking.domain.entity.BookingHistory;
import com.gymproject.booking.booking.domain.event.BookingChangedEvent;
import com.gymproject.booking.booking.infrastructure.persistence.BookingHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
    Booking -> BookingHistory 리스너임(내부용)
 */
@Component
@RequiredArgsConstructor
public class BookingChangedListener {

    private final BookingHistoryRepository bookingHistoryRepository;
    private static Logger log = LoggerFactory.getLogger(BookingChangedListener.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingChangedEvent(BookingChangedEvent event) {

        try {
            BookingHistory history = BookingHistory.builder()
                    .bookingId(event.getBooking().getBookingId())
                    .modifierId(event.getModifier().id())
                    .modifierRole(event.getModifier().role())
                    .actionType(event.getActionType())
                    .previousStatus(event.getPreviousStatus())
                    .newStatus(event.getNewStatus())
                    .reason(event.getReason())
                    .cancellationType(event.getCancellationType())
                    .bookingType(event.getBookingType())
                    .build();

            bookingHistoryRepository.save(history);

            log.info("Booking history 이벤트 리스너 실행 → action={}, bookingId={}, modifierId={}",
                    event.getActionType(),
                    event.getBooking().getBookingId(),
                    event.getModifier().id()
            );
        } catch (Exception e) {
            log.error("Booking history 이벤트 리스너 실패 bookingId={}, action={}, error={}",
                    event.getBooking().getBookingId(),
                    event.getActionType(),
                    e.getMessage(),
                    e);
            throw e; // Listener 실패를 외부로 알림
        }
    }
}

/*
     @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
      BOOKING_HISTORY_TB의 로직은 실패했지만, BOOKING 의 로직은 성공하였음.

      이러면 정합성이 안맞는거 아닌가? 왜 AFTER_COMMIT을 사용했는지??

      1. Befroe_commit으로 강한결합을 진행하면,핵심 비지니스의 트랜잭션이 깨뜨려짐.

      스프링 공식 문서에서도 다음과 같이 설명하고 있음

      도메인 이벤트 리스너는 다른 트랜잭션에서 실행되며, 본 트랜잭션을 절대 방해하지 않아야한다.

      그럼에도 히스토리 실패시 예약도 롤백시키고 싶다면 before_commit을 사용하면되나, 강합 결합이라서
      상황에 별로 어울리지 않음.
 */