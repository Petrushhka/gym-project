package com.gymproject.classmanagement.recurrence.infrastructure.persistence;

import com.gymproject.classmanagement.recurrence.domain.entity.RecurrenceGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecurrenceGroupRepository extends JpaRepository<RecurrenceGroup, Long> {

@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
SELECT r FROM RecurrenceGroup r WHERE r.groupId = :id
""")
  Optional<RecurrenceGroup>  findByIdWithLock(@Param("id") Long recurrenceGroupId);

@Query("""
    SELECT r FROM RecurrenceGroup r
    WHERE r.endDate IN :statuses
    AND r.endDate < :now
""")
    List<RecurrenceGroup> findExpiredGroups(
        @Param("statuses") List<String> statuses,
        @Param("now") LocalDate now);
}

/*
    PESSIMISTIC_WRITE을 하는 이유
    DB 수준에서 SELECT ... FOR UPDATE 쿼리를 날림

    이 때, 다른 트랜잭션이 읽거나 쓰려고하면, 기존 작업이 끝날때까지 대기하게 만듦

    왜냐하면 현재 작업 자체가 "정원 선점" 작업이기 때문임.
 */