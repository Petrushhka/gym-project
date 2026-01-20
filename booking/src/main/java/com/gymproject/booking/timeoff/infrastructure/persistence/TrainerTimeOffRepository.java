package com.gymproject.booking.timeoff.infrastructure.persistence;

import com.gymproject.booking.timeoff.domain.entity.TrainerTimeOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface TrainerTimeOffRepository extends JpaRepository<TrainerTimeOff, Long> {

    // 1. 파라미터로 날짜 객체 2개를 직접 받습니다.
    // 2. Postgres 함수 tstzrange를 사용하여 DB 내부에서 범위를 생성합니다.
    //    '[)' : 시작 포함, 종료 미포함 (Standard)
    //    && : 오버랩 연산자
    @Query(value = """
            SELECT count(*) > 0
            FROM "TRAINER_TIME_OFF_TB" t
            WHERE t.user_id = :userId
            AND t.status != 'CANCELLED'
            AND t.time_range && tstzrange(
                        CAST(:start AS timestamptz), 
                        CAST(:end AS timestamptz),
                                     '[)'
            )
            """, nativeQuery = true)
    boolean existsConflict(@Param("userId") Long userId,
                           @Param("start") OffsetDateTime start,
                           @Param("end") OffsetDateTime end);

    Optional<TrainerTimeOff> findByTrainerBlockId(Long trainerBlockId);

}
