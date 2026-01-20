package com.gymproject.common.port.classmanagement;

import com.gymproject.common.dto.schedule.ScheduleInfo;

import java.time.OffsetDateTime;
import java.util.List;

public interface ScheduleQueryPort {
    // 1] 1:1 예약시 "해당 시간에 트레이너의 시간이 비어있는지?
    void validateConflict(
            Long trainerId,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    );

    // 2. 수업 언제 시작, 종료하는지
    ScheduleInfo getScheduleInfo(Long scheduleId);

    // 3. 커리큘럼형 수업의 Id를 가지고 Schedule 정보를 조회
    // 커리큘럼형 수업의 예약을 한번에 등록
    List<ScheduleInfo> getScheduleInfos(Long recurrenceGroupId);

    // 4.
    OffsetDateTime getCurriculumEndAt(Long recurrenceGroupId);

    // 수업이 언제 종료하는지
    OffsetDateTime getScheduleEndAt(Long scheduleId);
    // 5. 가장 처음수업의 정보
    ScheduleInfo getFirstScheduleInCurriculum(Long recurrenceId);

}
