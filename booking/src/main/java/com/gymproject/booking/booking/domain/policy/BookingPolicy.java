package com.gymproject.booking.booking.domain.policy;

import com.gymproject.booking.booking.application.dto.request.TicketType;
import com.gymproject.booking.booking.domain.type.BookingStatus;
import com.gymproject.booking.booking.domain.type.CancellationType;
import com.gymproject.booking.booking.exception.BookingErrorCode;
import com.gymproject.booking.booking.exception.BookingException;

import java.time.Duration;
import java.time.OffsetDateTime;

public class BookingPolicy {

    // 1. 예약 생성 정책
    /*
        초기 예약 상태 결정(자동 승인 vs 대기)
        무료 체험권: 승인 대기(PENDING)
        유료 이용권: 즉시 확정(CONFIRMED)
     */
    public static BookingStatus determineInitialStatus(TicketType ticketType) {
        return ticketType.getInitialStatus();
    }

    /*
        예약 가능 시간 검증(Deadline Check)
        무료 체험권: 수업 3시간 전까지
        유료 이용권: 수업 1시간 전까지
     */
    public static  void validateBookingDeadline(TicketType ticketType, OffsetDateTime startAt, OffsetDateTime now) {
        int deadlineHours = ticketType.getDeadlineHours();

        OffsetDateTime deadline = startAt.minusHours(deadlineHours);

        if(now.isAfter(deadline)){
            throw new BookingException(BookingErrorCode.BOOKING_DEADLINE_EXCEEDED,
                    String.format("해당 이용권은 수업 시작 %d시간 전까지만 예약 가능합니다.", deadlineHours));
        }
    }


    // 2. 예약 취소,환불 정책( 1:1 PT에서만 사용)

    /*  예약 후
        1. 10분 이내: 무료 취소(실수 예약 방지)
        2. 24시간 전: 무료 취소
        3. 1시간 전: 예약금 취소(세션권은 사용된 것)
        4. 1시간 이내: 취소불가
     */
    public static CancellationType calculateCancellationType(OffsetDateTime bookedAt,
                                                      OffsetDateTime now,
                                                      OffsetDateTime classStartAt) {
        // 1. [실수 방지] 예약 후 10 분 이내라면 무조건 무료 취소
        if(Duration.between(bookedAt, now).toMinutes() <= 10){
            return CancellationType.FREE_CANCEL;
        }

        // 2. 수업 시작 24시간 이전: 무료취소
        if(now.isBefore(classStartAt.minusHours(24))){
            return CancellationType.FREE_CANCEL;
        }

        // 3. 수업 1시간 전 ~ 24시간 전 사이: 세션권 박탈,
        if(now.isBefore(classStartAt.minusHours(1))){
            return CancellationType.PENALTY_CANCEL;
        }

        // 4. 수업 1시간 이내: 취소 불가
        return CancellationType.IMPOSSIBLE;
    }

    //  커리큘럼(과정형) 수업 취소 가능 시간 검증
    public static void validateCurriculumCancellation(OffsetDateTime classStartAt,
                                                      OffsetDateTime now){
        if(now.isAfter(classStartAt.minusHours(3))){
            throw new BookingException(BookingErrorCode.CANCELLATION_NOT_ALLOWED, "커리큘럼 취소는 수업 3시간 전까지만 가능합니다.");
        }
    }

    // 출석은 수업 시작 15분전부터 가능
    public static void validateCheckInTime(OffsetDateTime classStartAt, OffsetDateTime now){
        if(now.isBefore(classStartAt.minusMinutes(15))){
            throw new BookingException(BookingErrorCode.CHECK_IN_TOO_EARLY);
        }
    }

    public static void validateCheckInDistance(double distance){
        double allowedDistance = 150.0;
        if(distance > allowedDistance){
            throw new BookingException(BookingErrorCode.CHECK_IN_DISTANCE_EXCEEDED);
        }
    }

}
