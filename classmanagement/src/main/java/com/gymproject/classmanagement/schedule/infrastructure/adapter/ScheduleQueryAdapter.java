package com.gymproject.classmanagement.schedule.infrastructure.adapter;

import com.gymproject.classmanagement.schedule.domain.entity.Schedule;
import com.gymproject.classmanagement.schedule.domain.service.ScheduleValidator;
import com.gymproject.classmanagement.schedule.exception.ScheduleErrorCode;
import com.gymproject.classmanagement.schedule.exception.ScheduleException;
import com.gymproject.classmanagement.schedule.infrastructure.persistence.ScheduleRepository;
import com.gymproject.common.dto.schedule.ScheduleInfo;
import com.gymproject.common.port.classmanagement.ScheduleQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static com.gymproject.common.constant.GymTimePolicy.SERVICE_ZONE;

/**
 * OutBound 포트로써의 Service역할을 다해야함
 */
@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
public class ScheduleQueryAdapter implements ScheduleQueryPort {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleValidator scheduleValidator;

    // 1] 충돌 검사
    @Override
    public void validateConflict(Long trainerId, OffsetDateTime startAt, OffsetDateTime endAt) {
        // 단순히 Service로 위임
        scheduleValidator.validateConflict(trainerId, startAt, endAt);
    }

    // 2] 단건조회
    @Override
    public ScheduleInfo getScheduleInfo(Long scheduleId) {
        Schedule schedule = getSchedule(scheduleId);
        return mapToScheduleInfo(schedule);
    }

    // 3] 그룹별 전체 조회
    @Override
    public List<ScheduleInfo> getScheduleInfos(Long recurrenceGroupId) {
        List<Schedule> schedules = scheduleRepository.findAllByRecurrenceGroupId(recurrenceGroupId);

        // 빈 리스트일 경우 예외를 던질지, 빈 리스트를 줄지는 정책 나름이지만,
        // 보통 그룹 ID로 조회했는데 아무것도 없으면 로직상 문제일 수 있음 (여기선 빈 리스트 허용 혹은 예외 처리)
        if (schedules.isEmpty()) {
            // 필요하다면 예외 처리: throw new ScheduleException(ScheduleErrorCode.NOT_FOUND);
        }

        return schedules.stream()
                .map(this::mapToScheduleInfo)
                .toList();
    }
    // 4] 커리큘럼 종료일 계산 (타입 변환 로직 포함)
    @Override
    public OffsetDateTime getCurriculumEndAt(Long recurrenceGroupId) {
        Object result = scheduleRepository.findMaxEndAtByGroupId(recurrenceGroupId)
                .orElseThrow(() -> new ScheduleException(ScheduleErrorCode.NOT_FOUND,
                        "커리큘럼 ID: " + recurrenceGroupId));

        // DB 드라이버나 버전에 따른 타입 불일치 해결 로직은 Service(Business) 레벨에서 처리하여 Adapter를 보호
        if (result instanceof Instant instant) {
            return instant.atZone(SERVICE_ZONE).toOffsetDateTime();
        }
        if (result instanceof Timestamp timestamp) {
            return timestamp.toInstant().atZone(SERVICE_ZONE).toOffsetDateTime();
        }
        return (OffsetDateTime) result;
    }

    //  5] 스케줄 종료 시간 조회
    @Override
    public OffsetDateTime getScheduleEndAt(Long scheduleId) {
        return getSchedule(scheduleId).getEndAt();
    }

    // 6]  커리큘럼의 첫 번째 스케줄 조회
    @Override
    public ScheduleInfo getFirstScheduleInCurriculum(Long recurrenceGroupId) {
        Schedule schedule = scheduleRepository.findByRecurrenceGroupIdFirstSchedule(recurrenceGroupId)
                .orElseThrow(() -> new ScheduleException(ScheduleErrorCode.NOT_FOUND,
                        "커리큘럼의 첫 수업을 찾을 수 없습니다. Group ID: " + recurrenceGroupId));

        return mapToScheduleInfo(schedule);
    }


    // ==========================================================
    //  SECTION 4: Helper Methods (Private)
    // ==========================================================

    private Schedule getSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(
                        ()-> new ScheduleException(ScheduleErrorCode.NOT_FOUND)
                );
    }

    // Entity -> DTO 변환 로직
    private ScheduleInfo mapToScheduleInfo(Schedule schedule) {
        return new ScheduleInfo(
                schedule.getClassScheduleId(),
                schedule.getTrainerId(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getStatus().name(),
                schedule.getCapacity(),
                schedule.getTemplate() == null ? "1:1 수업" : schedule.getTemplate().getTitle()
        );
    }
}
