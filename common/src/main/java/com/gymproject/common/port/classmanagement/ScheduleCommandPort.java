package com.gymproject.common.port.classmanagement;

import java.time.OffsetDateTime;

public interface ScheduleCommandPort {

    /// [중요]
    // 1] 1:1 PT 수업
    // ( 스케줄이 먼저 생성되어서 ScheduleID가 발생해야, Booking Entity도 만들수있음)
    Long createSingleSchedule(Long trainerId, OffsetDateTime startAt, OffsetDateTime endAt);

    // 동기적으로 처리하는 것이 안전
    // 2] 커리큘럼형 참여: 상태/정원을 체크하고 인원을 1씩 증가
    void reserveCurriculum(Long recurrenceGroupId);

    // 동기적으로 처리하는 것이 안전
    // 3] 루틴형 참여: 특정 수업 하나의 상태/정원 체크하고 인원을 1증가
    void reserveRoutine(Long scheduleId);

    // 4] 과정형 예약 취소(RecurrenceGroup 잔여석 복구)
    void cancelCurriculumReservation(Long recurrenceId);

    // 5] 단건/회차 예약 취소(Schedule 잔여석 복구)
    void cancelRoutineReservation(Long scheduleId);

}
