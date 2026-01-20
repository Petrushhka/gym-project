package com.gymproject.booking.booking.application.dto.request;

import com.gymproject.booking.booking.domain.policy.BookingConfirmPolicy;
import com.gymproject.booking.booking.domain.type.BookingStatus;
import com.gymproject.common.contracts.SessionConsumeKind;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketType {

    FREE_TRIAL(SessionConsumeKind.FREE_TRIAL, BookingConfirmPolicy.REQUIRE_APPROVAL, 3),
    PAID(SessionConsumeKind.PAID, BookingConfirmPolicy.AUTO_CONFIRM, 1);

    private final SessionConsumeKind sessionConsumeKind;
    private final BookingConfirmPolicy bookingConfirmPolicy;
    private final int deadlineHours; // 최종 예약 시간


    public SessionConsumeKind getSessionConsumeKind() {
        return sessionConsumeKind;
    }
    public BookingConfirmPolicy getBookingConfirmPolicy() {
        return bookingConfirmPolicy;
    }
    public BookingStatus getInitialStatus() {
        return this.bookingConfirmPolicy.getInitialStatus();
    }
}

/*
    이게 User 모듈에서
    SessionType과 같은 의미를 가지는 타입임.

    그러나 변화의 전파방지(Decoupling)을 위해 따로 의미가 같은 중복된 타입을 만든것임

    다음 예제 상황
    vip 전용 무료이용권을 추가하려고함, 무료지만 트레이너 승인 없이 바로 예약 확정이 되어야함.

    이런 경우에 TicketType과 SessionType은 의미가 서로 다름

    TicketType: 예약 로직의 분기 기준(즉시 확정 또는 대기)
    sessionType: 상품의 종류(무료냐 유료냐)
 */

/* 수정
    FREE_TRIAL(무료타입),
    PAID(유료타입)


    TicketType은 예약을 어떻게 확정지을지를 제어하고,
    SessionType은 어떤 세션권을 차감/복구할지를 제어하기에 둘이 다름.
 */