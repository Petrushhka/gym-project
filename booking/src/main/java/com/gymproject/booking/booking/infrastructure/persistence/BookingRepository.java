package com.gymproject.booking.booking.infrastructure.persistence;

import com.gymproject.booking.booking.domain.entity.Booking;
import com.gymproject.booking.booking.domain.type.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingId(Long bookingId);

    // Booking 엔티티에 scheduleId만 있다면, Schedule 테이블과 Join하여 필터링
    /*
        Booking.b 테이블에서 userId가 userId 파라미터와 같고,
        Schedule.s 테이블에서 recurrenceId가 reucrrenceId 파라미터와 같으면서
        상태가 확정인 스케줄이 하나 이상이면 예약이 된 것으로 판단
     */
    @Query("""
            SELECT COUNT(b) > 0
            FROM Booking b
            JOIN Schedule  s ON b.classScheduleId = s.classScheduleId
            WHERE b.userId = :userId
            AND s.recurrenceGroup.groupId = :recurrenceId
            AND b.status = 'CONFIRMED'
            """)
    boolean isAlreadyBooked(@Param("userId") Long userId, @Param("recurrenceId") Long recurrenceId);

    // 단일수업, 루틴형(원데이)
    // Booking, Schedule 조인하였음.
    /**
     * [커리큘럼(반복 예약) 일괄 조회를 위한 쿼리]
     * * 설계 의도:
     * 특정 사용자가 특정 커리큘럼(RecurrenceGroup)으로 신청한 모든 예약 내역을 한꺼번에 가져옵니다.
     * * 조인 전략:
     * Booking 엔티티에는 상세 수업 시간 정보가 없으므로, Schedule과 조인하여
     * 해당 스케줄이 속한 '반복 그룹 ID(RecurrenceId)'를 기준으로 필터링합니다.
     */
    @Query("""
    SELECT b 
    FROM Booking b 
    JOIN Schedule s ON b.classScheduleId = s.classScheduleId
    WHERE b.userId = :userId 
    AND s.recurrenceGroup.groupId = :recurrenceId
    """)
    List<Booking> findAllByUserIdAndRecurrenceId(@Param("userId") Long userId,
                                                 @Param("recurrenceId") Long recurrenceId);

    boolean existsByUserIdAndClassScheduleId(Long userId, Long scheduleId);

    boolean existsByUserIdAndClassScheduleIdAndStatusIn(Long userId, Long scheduleId, Collection<BookingStatus> statuses);

    List<Booking> findAllByClassScheduleId(Long scheduleId);

    List<Booking> findAllByClassScheduleIdAndStatus(Long scheduleId, BookingStatus bookingStatus);

}