package com.gymproject.readmodel.infrastructure.persistence;

import com.gymproject.readmodel.domain.TrainerCalendar;
import com.gymproject.readmodel.domain.type.CalendarSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TrainerCalendarRepository extends JpaRepository<TrainerCalendar, Long> {

    Optional<TrainerCalendar> findBySourceTypeAndSourceId(CalendarSource calendarSource, Long sourceId);

    // 기간 조회 쿼리
    // 요청 기간(start~end)과 겹치는(overlap) 모든 데이터를 조회
    @Query(value = """
        SELECT * FROM trainer_calendar_r t
        WHERE t.trainer_id = :trainerId
        AND t.time_range && tstzrange(CAST(:start AS timestamptz), CAST(:end AS timestamptz), '[)')
        ORDER BY lower(t.time_range) ASC
    """, nativeQuery = true)
    List<TrainerCalendar> findAllByTrainerIdAndPeriod(
            @Param("trainerId") Long trainerId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );
}
