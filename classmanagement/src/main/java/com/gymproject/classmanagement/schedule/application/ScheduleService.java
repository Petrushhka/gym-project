package com.gymproject.classmanagement.schedule.application;

import com.gymproject.classmanagement.recurrence.domain.entity.RecurrenceGroup;
import com.gymproject.classmanagement.schedule.application.dto.ScheduleResponse;
import com.gymproject.classmanagement.schedule.domain.entity.Schedule;
import com.gymproject.classmanagement.schedule.domain.service.ScheduleGenerator;
import com.gymproject.classmanagement.schedule.domain.service.ScheduleValidator;
import com.gymproject.classmanagement.schedule.exception.ScheduleErrorCode;
import com.gymproject.classmanagement.schedule.exception.ScheduleException;
import com.gymproject.classmanagement.schedule.infrastructure.persistence.ScheduleRepository;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.port.auth.IdentityQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleValidator scheduleValidator;
    private final ScheduleGenerator scheduleGenerator;
    private final IdentityQueryPort identityQueryPort;

    // ==========================================================
    //  SECTION 1: Trainer Actions (트레이너 직접 요청)
    // ==========================================================


    // 추가] Listener를 위한 로직(그룹 기반 스케줄 생성) Recurrnece 생성 -> Schedule 생성
    @Transactional
    public void createSchedulesFromGroup(RecurrenceGroup group) {
        //1. 생성
        List<Schedule> schedules = scheduleGenerator.generateSchedules(group, group.getTemplate());

        if (schedules.isEmpty()) return;

        // 2. 검증 (더블 체크가 되긴하지만 상관)
        scheduleValidator.validateConflicts(group.getTrainerId(), schedules);

        scheduleRepository.saveAll(schedules);
    }

    // 1] 수업 취소(PERSONAL 수업) - 강제 취소(트레이너 요청)
    @Transactional
    public ScheduleResponse cancelSchedule(Long scheduleId, UserAuthInfo userAuthInfo) {
        // 1. 트레이너 신원 검증
        identityQueryPort.validateTrainer(userAuthInfo.getUserId());
        // 2. 스케줄 조회
        Schedule schedule = getSchedule(scheduleId);

        // 3. 소유권 확인
        validateOwner(schedule, userAuthInfo.getUserId());

        // 강제 취소 = true, 마감 시간 = 0
        schedule.cancel(true, 0);

        // save를 호출하지 않아도 @Transactional으로 인해 더티체킹으로 업데이트됨. 허나 명식적으로 적음.
        scheduleRepository.save(schedule);

        return ScheduleResponse.create(schedule);
    }

    // 2] 그룹 전체 강제 폐강(트레이너 요청)
    @Transactional
    public void cancelSchedulesByRecurrence(Long recurrenceId) {
        // 1.  해당 그룹에 속한 모든 하위 스케쥴 조회
        List<Schedule> schedules =
                scheduleRepository.findAllByRecurrenceGroupId(recurrenceId);

        if (schedules.isEmpty()) {
            throw new ScheduleException(ScheduleErrorCode.NOT_FOUND, "해당 그룹의 스케줄이 존재하지 않습니다.");
        }

        // 2. 모든 자식 스케줄 폐강 처리
        for (Schedule schedule : schedules) {
            schedule.cancel(true, 0);
        }

        // 3. 저장
        scheduleRepository.saveAll(schedules);
    }

    // ==========================================================
    //  SECTION 2: System / Adapter Commands (타 모듈 연동)
    // ==========================================================

    // 3] 단일 스케줄 생성(Personal) - Booking에 의해서 생성
    @Transactional
    public Long createSingleSchedule(Long trainerId,
                                     OffsetDateTime startAt,
                                     OffsetDateTime endAt) {
        Schedule schedule = Schedule.createPersonal(trainerId, startAt, endAt);
        scheduleRepository.save(schedule);
        return schedule.getClassScheduleId();
    }


    // 4] 단건 스케줄(Routine/Personal) 예약 (좌석 차감)
    @Transactional
    public void reserveRoutine(Long scheduleId){
        Schedule schedule = getSchedule(scheduleId);

        // 0명까지 예약 가능 (Personal 수업 고려)
        schedule.decreaseCount(0);

        scheduleRepository.save(schedule);
    }

    // 5] 단건 스케줄(Routine/Personal) 예약 취소(좌석 복구)
    @Transactional
    public void cancelRoutineReservation(Long scheduleId){
        Schedule schedule = getSchedule(scheduleId);

        schedule.increaseCount();

        scheduleRepository.save(schedule);
    }

    // 6] 예약취소에 의한 단일 스케줄 폐강(PERSONAL)
    @Transactional
    public void closeScheduleBySystem(Long scheduleId) {
        Schedule schedule = getSchedule(scheduleId);

        schedule.closePersonal();

        scheduleRepository.save(schedule);
    }

    // 7] 달력용 스케줄 조회(월별 조회)
    public List<ScheduleResponse> getMonthlySchedules(Long trainerId, LocalDate startDate, LocalDate endDate) {

        // 시간 변환 (LocalDate -> OffsetDateTime)
        // 시작일 00:00:00 ~ 종료일 23:59:59
        // (실무에서는 ZoneId를 받아서 처리해야 정확하지만, 여기선 편의상 UTC/ServerTime 기준)
        OffsetDateTime startAt = startDate.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        OffsetDateTime endAt = endDate.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        // Reader를 통해 조회 (Repository 직접 호출보다 Reader 경유 추천)
        List<Schedule> schedules = scheduleRepository.findSchedulesByPeriod(trainerId, startAt, endAt);

        // Entity -> DTO 변환
        return schedules.stream()
                .map(ScheduleResponse::create)
                .collect(Collectors.toList());
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

    private static void validateOwner (Schedule schedule, Long userId) {
        if (!schedule.getTrainerId().equals(userId)) {
            throw new ScheduleException(ScheduleErrorCode.ACCESS_DENIED);
        }
    }

    public void syncSchedulesWithGroup(Long recurrenceGroupId, RecurrenceGroup group) {
        List<Schedule> schedules = scheduleRepository.findAllByRecurrenceGroupId(recurrenceGroupId);

        if (schedules.isEmpty()) {
            // 로직상 그룹은 있는데 스케줄이 하나도 없는 경우 (데이터 불일치 가능성 경고 로그 등을 남길 수 있음)
            return;
        }

        for (Schedule schedule : schedules) {
            schedule.mirrorParentStatus(group.getRemainingCapacity(), group.getRecurrenceStatus());
        }

        scheduleRepository.saveAll(schedules);
    }

}