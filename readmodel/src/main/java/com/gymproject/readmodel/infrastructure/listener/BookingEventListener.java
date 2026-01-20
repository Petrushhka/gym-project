//package com.gymproject.readmodel.infrastructure.listener;
//
//import com.gymproject.common.event.domain.BookingEvent;
//import com.gymproject.common.port.user.UserQueryPort;
//import com.gymproject.readmodel.domain.CalendarStatus;
//import com.gymproject.readmodel.domain.TrainerCalendar;
//import com.gymproject.readmodel.domain.type.CalendarSource;
//import com.gymproject.readmodel.infrastructure.persistence.TrainerCalendarRepository;
//import io.hypersistence.utils.hibernate.type.range.Range;
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.ZonedDateTime;
//
//import static com.gymproject.common.constant.GymTimePolicy.SERVICE_ZONE;
//
//@Component
//@RequiredArgsConstructor
//public class BookingEventListener {
//
//    private final TrainerCalendarRepository trainerCalendarRepository;
//    private final UserQueryPort userQueryPort;
//
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
//    @EventListener
////    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)  << 이미 첫번째 Listener에서 트랜잭션이 종료됬는데 그 이후에 트랜잭션이 없으니까 발생이 안되는 현상이 발생했음.
//    public void handle(BookingEvent event) {
//
//        CalendarStatus newStatus = CalendarStatus.mapStatus(event);
//        ZonedDateTime zonedStart = event.startAt().atZoneSameInstant(SERVICE_ZONE);
//        ZonedDateTime zonedEnd = event.endAt().atZoneSameInstant(SERVICE_ZONE);
//        Range<ZonedDateTime> range = Range.closedOpen(zonedStart, zonedEnd);
//
//        // Booking과 PK를 통해 관련된 일정을 조회
//        TrainerCalendar existing =
//                trainerCalendarRepository.findBySourceTypeAndSourceId(
//                        CalendarSource.BOOKING,
//                        event.bookingId()
//                ).orElse(null);
//
//        if (existing == null) {
//            // Booking과 관련된 내용이 TrainerCalendar에 등록되지 않았다면, 등록
//            create(event, range, newStatus);
//        } else {
//            // 기존 Row 업데이트
//            update(existing, newStatus);
//        }
//    }
//
//    private void create(
//            BookingEvent event,
//            Range<ZonedDateTime> range,
//            CalendarStatus newStatus
//    ) {
//        // 1. User 모듈에서 이름 조회
//        String trainerName = userQueryPort.getTrainerName((event.trainerId()));
//
//        // 2. 타이틀
//        String title = "개인 PT 예약";
//
//        // 3. 엔티티 생성
//        TrainerCalendar calendar = TrainerCalendar.createBookingEvent(
//                event,
//                trainerName,
//                title,
//                range,
//                newStatus);
//
//        trainerCalendarRepository.save(calendar);
//    }
//
//    private void update(TrainerCalendar existing, CalendarStatus newStatus) {
//        existing.updateStatus(newStatus);
//
//        trainerCalendarRepository.save(existing);
//    }
//
//}

/*
    모든 수업예약이 Schedule과 연동되므로 Schedule 이벤트로 관리하면됨.[중요]
 */