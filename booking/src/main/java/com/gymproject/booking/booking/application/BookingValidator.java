package com.gymproject.booking.booking.application;

import com.gymproject.booking.booking.domain.type.BookingStatus;
import com.gymproject.booking.booking.exception.BookingErrorCode;
import com.gymproject.booking.booking.exception.BookingException;
import com.gymproject.booking.booking.infrastructure.persistence.BookingRepository;
import com.gymproject.common.port.auth.IdentityQueryPort;
import com.gymproject.common.port.classmanagement.ScheduleQueryPort;
import com.gymproject.common.port.user.UserMembershipPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingValidator {

    private final IdentityQueryPort identityQueryPort;
    private final UserMembershipPort userMembershipPort;
    private final ScheduleQueryPort scheduleQueryPort;
    private final BookingRepository bookingRepository;


    // 1. 트레이너 검증
    public void validateTrainer(Long trainerId){
            identityQueryPort.validateTrainer(trainerId);
    }

    // 2. 멤버십 회원 검증
    public void validateMembership(Long userId){
        identityQueryPort.validateMembershipUser(userId);
    }

    // 2. 스케줄 충돌 검증
    public void validateScheduleConflict(Long trainerId,
                                         OffsetDateTime startAt,
                                         OffsetDateTime endAt){
        scheduleQueryPort.validateConflict(trainerId, startAt, endAt);
    }

    // 3. 멤버십 유효기간 검증
    public void validateMembershipActiveUtill(Long userId, OffsetDateTime classEndAt){
        userMembershipPort.validateMembershipUntil(userId, classEndAt);
    }

    // 4. 중복 예약 검증(커리큘럼)
    public void validateDuplicateCurriculum(Long userId, Long recurrenceId){
        if(bookingRepository.isAlreadyBooked(userId, recurrenceId)){
            throw new BookingException(BookingErrorCode.ALREADY_BOOKED_CURRICULUM);
        }
    }

    // 5. 중복 예약 검증 (단건 스케줄)
    public void validateDuplicateSchedule(Long userId, Long scheduleId){
        // 1. 중복 예약 검증 (취소된 예약은 제외하고 체크)
        // 유효한 예약 상태들 (예: 확정, 승인대기 등)
        List<BookingStatus> activeStatuses = List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING);
        if(bookingRepository.existsByUserIdAndClassScheduleIdAndStatusIn(userId, scheduleId,activeStatuses)){
            throw new BookingException(BookingErrorCode.ALREADY_BOOKED_SCHEDULE);
        }
    }
}
