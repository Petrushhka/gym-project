package com.gymproject.classmanagement.schedule.infrastructure.persistence;

import com.gymproject.classmanagement.schedule.domain.entity.Schedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {


//    List<Schedule> findAllByTrainerIdAndStartAtBetween(Long trainerId,
//                                                       OffsetDateTime startAtAfter,
//                                                       OffsetDateTime startAtBefore);

    @Query(value = """
                SELECT * FROM "CLASS_SCHEDULE_TB" s
                WHERE s.trainer_id = :trainerId
                -- time_range가 요청된 기간 start/end를 완전히 포함하거나 겹치는 모든 스케줄 조회
                AND s.time_range && tstzrange(:startAt, :endAt, '[)')
            """, nativeQuery = true)
    List<Schedule> findAllByTrainerIdAndStartAtBetween(@Param("trainerId") Long trainerId,
                                                       @Param("startAt") OffsetDateTime startAt,
                                                       @Param("endAt") OffsetDateTime endAt);

    Optional<Schedule> findByClassScheduleId(Long classScheduleId);

    //case when x = y then a else b end
    // 조건 x=y 가 true 일 경우 a이고 그렇지 않으면 b
    @Query(value = """
                SELECT EXISTS(
                SELECT 1 FROM "CLASS_SCHEDULE_TB" s
                WHERE s.trainer_id = :trainerId
                AND s.status != 'CANCELLED' 
                AND s.time_range && tstzrange(
                            CAST(:startAt AS timestamptz), 
                            CAST(:endAt AS timestamptz),
                                                     '[)')
                )
            """, nativeQuery = true)
    boolean existsConflict(
            @Param("trainerId") Long trainerId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt);

    @Query(value = """
                SELECT lower(time_range)
                FROM "CLASS_SCHEDULE_TB"
                WHERE class_schedule_id = :id
            """, nativeQuery = true)
    Optional<OffsetDateTime> findStartTimeById(@Param("id") Long id);


    // recurrence_group_id를 가지고 Schedule을 모두 조회
    @Query("SELECT s FROM Schedule s WHERE " +
            "s.recurrenceGroup.groupId = :recurrenceGroupId")
    List<Schedule> findAllByRecurrenceGroupId(@Param("recurrenceGroupId") Long recurrenceGroupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE) // 다른 트랜잭션이 읽기/쓰기를 못하게 막음
    @Query("SELECT s FROM Schedule s WHERE s.recurrenceGroup.groupId = :recurrenceGroupId")
    List<Schedule> findAllByRecurrenceGroupIdWithLock(@Param("recurrenceGroupId") Long recurrenceGroupId);

    // 해당 그룹에서 가장 늦은 종료 시간을 가져오는 Native Query

    /***[중요]
     * 현재 쿼리의 문제점: Postgred에서 timestamptz 를 Native Query로 Max() 처리했을때,
     * 하이버네이트가 자바의 java.time.Instant타입으로 반환함.(우리가 필요한건 OffsetDateTime)
     *
     * Instant: 타임존 정보가 없는 UTC 기준의 시간을 나타내는 객체,
     * OffsetDateTime: 타임존 오프셋이 포함된 객체
     *
     */
    @Query(value = """
                SELECT MAX(upper(time_range))
                FROM "CLASS_SCHEDULE_TB"
                WHERE recurrence_group_id = :recurrenceId
            """, nativeQuery = true)
    Optional<Object> findMaxEndAtByGroupId(@Param("recurrenceId") Long recurrenceId);

    @Query(value = """
            SELECT s.* FROM "CLASS_SCHEDULE_TB" s
            WHERE s.recurrence_group_id = :recurrenceId
            ORDER By lower(time_range) ASC 
            LIMIT 1
            """, nativeQuery = true)
    Optional<Schedule> findByRecurrenceGroupIdFirstSchedule(@Param("recurrenceId") Long recurrenceId);

    @Query(value = """
                SELECT s.* FROM "CLASS_SCHEDULE_TB" s
                WHERE s.status IN (:statuses) 
                AND upper(s.time_range) < :now
            """, nativeQuery = true)
    List<Schedule> findExpiredClasses(
            @Param("statuses") List<String> statuses,
            @Param("now") LocalDate now);


    @Query(value = """
            SELECT * FROM "CLASS_SCHEDULE_TB" s
            WHERE s.trainer_id = :trainerId
            AND lower(s.time_range) >= :startDate 
            AND lower(s.time_range) <= :endDate
            ORDER BY lower(s.time_range) ASC
            """, nativeQuery = true)
    List<Schedule> findSchedulesByPeriod(@Param("trainerId") Long trainerId,
                                         @Param("startDate") OffsetDateTime startAt,
                                         @Param("endDate") OffsetDateTime endAt);

}


/**
 * 시간 범위 겹침 공식(Time overlap 체크, 예약 시간 충돌 공식, 구간 겹칙 공식 등)
 * <p>
 * !!! MySql 같은 두개의 컬럼으로 검사해야하는 경우 (거의 표준이되는 식)
 * <p>
 * 기존 시작시간 < 새예약 종료시간
 * &&
 * 기존 종료시간 > 새예약 시작시간
 * <p>
 * - 기존이 새예약을 감싸는 경우
 * - 새예약이 기존을 감싸는 경우
 * - 왼쪽이 일부 겹침
 * - 오른쪽 일부 겹침
 * - 시작점만 닿는 경우
 * - 끝점만 닿는 경우
 * - 완전히 안에 들어가는 경우
 */