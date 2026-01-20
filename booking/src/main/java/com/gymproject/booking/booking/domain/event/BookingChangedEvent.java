package com.gymproject.booking.booking.domain.event;

import com.gymproject.booking.booking.domain.entity.Booking;
import com.gymproject.booking.booking.domain.type.BookingActionType;
import com.gymproject.booking.booking.domain.type.BookingStatus;
import com.gymproject.booking.booking.domain.type.BookingType;
import com.gymproject.booking.booking.domain.type.CancellationType;
import com.gymproject.common.vo.Modifier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BookingChangedEvent{
    private final Booking booking; // BookingId를 넣으면 DB 저장되기전에 이벤트가 발생해서 Id 값이 null인 상태가 됨. 따라서 Book으로 변경
    private final Modifier modifier;
    private final BookingStatus previousStatus;
    private final BookingStatus newStatus;
    private final BookingActionType actionType;
    private final String reason;
    private final BookingType bookingType;
    private final CancellationType cancellationType; // 리스너에서 환불을 결정해줄 타입임

    public BookingChangedEvent(Builder builder) {
        this.booking = builder.booking;
        this.modifier = builder.modifier;
        this.previousStatus = builder.previousStatus;
        this.newStatus = builder.newStatus;
        this.actionType = builder.actionType;
        this.reason = builder.reason;
        this.bookingType = builder.bookingType;
        this.cancellationType = builder.cancellationType;
    }
    /**
     * 사용자의 입력값을 조립하는 객체가 아닌, 변경 기록을 저장하는 객체임
     * 따라서 Booking, Modifier는 이미 정해져있는 값을 사용해야하므로 final
     * 나머지 필드는 도메인 규칙에 의해 결정되어야하는 값이니 final하지 않음.
     */
    @Getter
    public static class Builder {
        private final Booking booking;
        private final Modifier modifier;
        private BookingStatus previousStatus;
        private BookingStatus newStatus;
        private BookingActionType actionType;
        private String reason;
        private BookingType bookingType;
        private CancellationType cancellationType;

        public Builder(Booking booking, Modifier modifier) {
            this.booking = booking;
            this.modifier = modifier;
            this.bookingType = booking.getBookingType();
        }

        public Builder actionType(BookingActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder status(BookingStatus previousStatus, BookingStatus newStatus) {
            this.previousStatus = previousStatus;
            this.newStatus = newStatus;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder bookingType(BookingType bookingType) {
            this.bookingType = bookingType;
            return this;
        }

        public Builder cancellationType(CancellationType type) {
            this.cancellationType = type;
            return this;
        }

        public BookingChangedEvent build() {
            return new BookingChangedEvent(this);
        }
    }

    // 예약 생성
    public static BookingChangedEvent created(Booking booking, Modifier modifier) {
        return new Builder(booking, modifier)
                .actionType(BookingActionType.CREATE)
                .status(null, booking.getStatus())
                .bookingType(booking.getBookingType())
                .reason("예약 생성")
                .build();
    }

    // 예약 승인
    public static BookingChangedEvent approved(Booking booking, Modifier mmodifier) {
        return new Builder(booking, mmodifier)
                .actionType(BookingActionType.APPROVE)
                .status(BookingStatus.PENDING, BookingStatus.CONFIRMED)
                .build();
    }

    // 예약 거절
    public static BookingChangedEvent rejected(Booking booking, Modifier modifier, String reason) {
        return new Builder(booking, modifier)
                .actionType(BookingActionType.REJECT)
                .status(BookingStatus.PENDING, BookingStatus.REJECTED)
                .reason(reason)
                .build();
    }

    // 취소
    public static BookingChangedEvent cancelled(Booking booking, Modifier modifier,
                                                CancellationType type,String reason) {
        return new Builder(booking, modifier)
                .actionType(BookingActionType.CANCEL)
                .status(booking.getStatus(), BookingStatus.CANCELLED)
                .cancellationType(type)
                .reason(reason)
                .build();
    }

    // 참석
    public static BookingChangedEvent attended(Booking booking, Modifier modifier) {
        return new Builder(booking, modifier)
                .actionType(BookingActionType.ATTEND)
                .status(booking.getStatus(), BookingStatus.ATTENDED)
                .build();
    }

    // 노쇼
    public static BookingChangedEvent noShow(Booking booking, Modifier modifier, String reason) {
        return new Builder(booking, modifier)
                .actionType(BookingActionType.NOSHOW)
                .status(booking.getStatus(),BookingStatus.NOSHOW)
                .reason(reason)
                .build();
    }
}

/**
 * Spring Event 사용 주석(UserSession)
 *
 * @see com.gymproject.user.event.SessionChangedEvent
 * <p>
 * Sessionr과 Booking 의 경우 Spring Event의 의도,발생 위치, 책임 분리, 필요 정보가 다름
 * <p>
 * SessionChangedEvent는 이벤트를 Service 레이어에서 발행함. (applicationEventPublisher.publishEvent(SessionChangedEvent.use(...))
 * <p>
 * 위와 같은 방식의 특징은
 * <p>
 * 1. 도메인은 단순히 값을 변경하는 객체: 이벤트 DB 반영후 발생해야함.
 * 2. Service 레이어가 모든 책임을 갖음: 서비스 레이어가 모든 책임을 갖음.
 * 3. 이벤트는 단순 후처리 목적: History 저장
 * <p>
 * BookingHistory Event의 방식은
 * Service가 아니라 도메인(Booking)이 직접 이벤트를 발행하는 구조임.
 * <p>
 * 예약 상태의 변경은 도메인이 가장 잘알고 있음.
 * <p>
 * 만약 서비스에서 이런 이벤트 발행을 하면,
 * <p>
 * 이전 상태가 어떤 상태인지 서비스가 추적해야하고, 변경해야할 상태와 비교도 해야함.
 * <p>
 * session의 경우에는 단순 숫자 차감이므로 도메인이 관리할 정보가 적음.
 * 그러나 booking은 상태 변경이 핵심 규칙이므로 도메인이 이벤트를 만들어야함.
 * <p>
 * Booking 엔티티 내부에서 Spring Class를 직접 사용하면 안 되므로 중간에 DomainDevents 유틸을 둔다.
 * <p>
 * DomainEvents.rasie(event);
 *
 *
 *
 *
 *
 */
