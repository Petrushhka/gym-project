package com.gymproject.classmanagement.schedule.domain.service;

import com.gymproject.classmanagement.schedule.domain.entity.Schedule;
import com.gymproject.classmanagement.schedule.exception.ScheduleErrorCode;
import com.gymproject.classmanagement.schedule.exception.ScheduleException;
import com.gymproject.classmanagement.schedule.infrastructure.persistence.ScheduleRepository;
import com.gymproject.common.port.booking.TimeOffQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduleValidator {

    private final ScheduleRepository scheduleRepository;
    private final TimeOffQueryPort timeOffQueryPort;

    ///  연속수업 생성시 검증
    public void validateConflicts(Long trainerId, List<Schedule> newSchedules) {
        if (newSchedules == null || newSchedules.isEmpty()) return;

        for (Schedule schedule : newSchedules) {
            // [중요] N+1 문제 발생
            boolean hasConflict = scheduleRepository.existsConflict(
                    trainerId,
                    schedule.getStartAt(),
                    schedule.getEndAt()
            );

            if (hasConflict) {
                throw new ScheduleException(ScheduleErrorCode.SCHEDULE_CONFLICT , schedule.getStartAt());
            }

            timeOffQueryPort.validateNoTimeOffOverlap(trainerId,schedule.getStartAt(),schedule.getEndAt());
        }
    }

    /// 1:1 예약 시 (단건 검증)
    public void validateConflict(Long trainerId, OffsetDateTime startAt, OffsetDateTime endAt) {
        boolean hasConflict = scheduleRepository.existsConflict(
                trainerId, startAt, endAt);
        if (hasConflict) throw new ScheduleException(ScheduleErrorCode.SCHEDULE_CONFLICT);

        timeOffQueryPort.validateNoTimeOffOverlap(trainerId,startAt,endAt);
    }

}

/*  기존 서비스레이어에 있던 메서드형태: 이중for문으로 두번 돌지 않고, 쿼리로 처리하는것이 좋음!!
     1) 이중for문: 별로 안좋음 << 기존
     2) JPA: 복잡해짐
     3) QuerySQL: 가장 베스트 << 이걸로선택했음

private void validateConflict(Long trainerId, List<Schedule> newSchedules) {
        if (newSchedules.isEmpty()) return;

        // DB 부하를 줄이기 위해 기간 내 모든 기존 스케줄을 한 번에 가져옵니다.
        OffsetDateTime minDate = newSchedules.get(0).getStartAt();
        OffsetDateTime maxDate = newSchedules.get(newSchedules.size() - 1).getEndAt();

        log.debug("시작시간: {}, 끝나는시간: {}", minDate, maxDate);

        // [주의] 이 findAllByTrainerIdAndStartAtBetween 메서드는 ClassScheduleRepository에 정의되어야 합니다.
        List<Schedule> existingSchedules = scheduleRepository
                .findAllByTrainerIdAndStartAtBetween(trainerId, minDate, maxDate);

        for (Schedule newSchedule : newSchedules) {
            for (Schedule existing : existingSchedules) {
                // 이미 취소된 수업이나, 자신과 같은 그룹에 속하는 수업(업데이트 시)은 제외할 수 있습니다.
                if (existing.getStatus() == ScheduleStatus.CANCELED) continue;

                // 시간 겹침 공식: (A_start < B_end) AND (A_end > B_start)
                // 새로등록할 수업의 시작시간이 기존있는 수업보다 이전이고 새로등록하는 수업의 마지막시간이 기존의 수업시작시간보다 후에있다면
                if (newSchedule.getStartAt().isBefore(existing.getEndAt()) &&
                        newSchedule.getEndAt().isAfter(existing.getStartAt())) {

                    throw new IllegalStateException(
                            "해당 시간에 이미 다른 수업이 존재합니다. 충돌 시간: " +
                                    newSchedule.getStartAt().atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                            // 충돌 시, 사용자가 이해하기 쉬운 체육관 시간대로 변환하여 에러 메시지 출력
                    );
                }
            }
        }
    }




        2. 추가

        timeOffQueryPort.validateNoTimeOffConlfict를 원래는 서비스 단에서 사용하려고 했으나,
        Validator로 옮긴이유는 비일관성 때문임. 수업 예약의 충돌 검사를 담당하는 validator에서 한번에 진행해야지
        두개로 쪼개버리면 혼선이 생길 수 있음(validate을 두번하는 것 처럼 보이기도 함. 서비스 레이어에서는)

 */