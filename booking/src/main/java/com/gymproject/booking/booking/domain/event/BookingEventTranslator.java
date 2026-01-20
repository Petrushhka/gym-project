package com.gymproject.booking.booking.domain.event;


import com.gymproject.booking.booking.domain.entity.Booking;
import com.gymproject.common.dto.schedule.ScheduleInfo;
import com.gymproject.common.event.domain.BookingEvent;
import com.gymproject.common.port.classmanagement.ScheduleQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 내부 이벤트 -> 외부 이벤트로 바꿔주는 용도
/**
 * 현재 클래스는 내부이벤트를 감지하는 리스너인 동시에 이벤트를 발생하는 퍼블리셔 이기도 함.
 * 동시에 수행하는 클래스임!!
 */
/**
 * [이벤트 번역기: Domain Event -> Integration Event]
 * * 이 클래스는 Booking 도메인 내부에서 발생한 '도메인 이벤트'를 구독하여,
 * 다른 모듈(스케줄, 알림 등)이 이해할 수 있는 '통합 이벤트(External Event)'로 변환하여 전파합니다.
 * * 목적:
 * 1. 도메인 엔티티(Booking)가 외부 시스템의 데이터 구조를 알지 못하게 분리 (결합도 감소)
 * 2. 예약 정보 외에 외부 모듈 처리에 필요한 추가 데이터(스케줄 정보 등)를 보강(Enrichment)
 */
@Component
@RequiredArgsConstructor
public class BookingEventTranslator {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ScheduleQueryPort scheduleQueryPort;

    /**
     * 내부 도메인 이벤트(BookingChangedEvent)를 수신하여 외부 통합 이벤트(BookingEvent)로 변환 및 발행합니다.
     * * [트랜잭션 관리 전략]
     * 1. @TransactionalEventListener(phase = AFTER_COMMIT):
     * - 메인 예약 로직이 '성공적으로 DB에 커밋'된 직후에만 실행됩니다.
     * - 예약이 실패(롤백)했는데 후속 작업(알림 발송, 스케줄 변경 등)이 실행되는 것을 방지합니다.
     * * 2. @Transactional(propagation = Propagation.REQUIRES_NEW):
     * - 메인 트랜잭션은 이미 커밋되어 닫혔으므로, 스케줄 정보 조회를 위해 '새로운 트랜잭션'을 시작합니다.
     * - 이를 통해 커밋 이후 시점에서도 안전하게 DB 커넥션을 확보하여 데이터를 보강할 수 있습니다.
     */

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void translate(BookingChangedEvent internalEvent) {

        // 1. 원본 도메인 데이터 추출
        Booking booking = internalEvent.getBooking();
        Long scheduleId = booking.getClassScheduleId();

        // 2. 데이터 보강 (Data Enrichment): Booking에는 없는 스케줄 상세 정보(시간, 강사 등)를 조회
        ScheduleInfo scheduleInfo = scheduleQueryPort.getScheduleInfo(scheduleId);

        // 3. 필드 가공 및 Null 방어 로직 (NPE 방지)
        // - 초기 예약 생성 시에는 '이전 상태'가 없으므로 null 체크 처리
        String prevStatus = internalEvent.getPreviousStatus() != null
        ? internalEvent.getPreviousStatus().name() : null;

        // - 거절(REJECT) 등의 상황에서는 취소 정책(CancellationType)이 없으므로 null 체크 처리
        String cancellationTypeName = (internalEvent.getCancellationType() != null)
                ? internalEvent.getCancellationType().name()
                : null;

        // 4. 통합 이벤트(Record형태의 DTO)로 변환
        BookingEvent externalEvent = new BookingEvent(
                booking.getBookingId(),
                scheduleInfo.trainerId(),
                scheduleId,
                scheduleInfo.startAt(),
                scheduleInfo.endAt(),
                internalEvent.getActionType().name(),
                prevStatus,
                internalEvent.getNewStatus().name(),
                internalEvent.getReason(),
                booking.getUserSessionId(),
                cancellationTypeName,
                booking.getBookingType().name()
        );

        // 외부로 이벤트 발행
        applicationEventPublisher.publishEvent(externalEvent);
    }

}
