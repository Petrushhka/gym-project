package com.gymproject.booking.timeoff.application;

import com.gymproject.booking.booking.application.dto.reponse.TimeOffResponse;
import com.gymproject.booking.timeoff.application.dto.TimeOffRequest;
import com.gymproject.booking.timeoff.domain.entity.TrainerTimeOff;
import com.gymproject.booking.timeoff.exception.TimeOffErrorCode;
import com.gymproject.booking.timeoff.exception.TimeOffException;
import com.gymproject.booking.timeoff.infrastructure.persistence.TrainerTimeOffRepository;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.port.auth.IdentityQueryPort;
import com.gymproject.common.port.classmanagement.ScheduleQueryPort;
import com.gymproject.common.util.GymDateUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class TrainerTimeOffService {

    private final TrainerTimeOffRepository trainerTimeOffRepository;
    private final IdentityQueryPort identityQueryPort;
    private final ScheduleQueryPort scheduleQueryPort;

    @Transactional
    public TimeOffResponse blockSchedule(TimeOffRequest request,
                                         UserAuthInfo userAuthInfo) {

        // 1. 토큰에서 유저권한 검사
        identityQueryPort.validateTrainer(userAuthInfo.getUserId());

        // 2. 시간대 변환
        OffsetDateTime startAt = request.getStartDateTime();
        OffsetDateTime endAt = request.getEndDateTime();

        // 3. 시작시간 종료시간 확인
        request.validateConflict(startAt, endAt, GymDateUtil.now());

        // 4. 본인 시간 중 중복으로 막아놓은 시간이 있는지 -> 이건 왜 Entity로 안가져가는지?? -> 외부 정합성에 맞는거라서 이건 넣을 필요없음.
        validateNoTimeOffOverlap(userAuthInfo.getUserId(),
                startAt, endAt);

        // 4. 해당 시간대에 다른 예약이 있는지 확인
        // -> 시작/종료 시간 확인은 여기서 확인하는게 더 좋지않을까? 한번에??
        // ->> 다른 포트에서 하는거보다 바로 처리해주는게 Fail Fast 효과가 있어서 더 나음
        scheduleQueryPort.validateConflict(userAuthInfo.getUserId(), startAt, endAt);

        // 5. Entity 변환
        TrainerTimeOff timeOff = TrainerTimeOff.create(userAuthInfo.getUserId(),
                startAt,
                endAt,
                request.getTimeOffType(),
                request.getReason());

        // 6. 저장
        trainerTimeOffRepository.save(timeOff);

        // 응답 객체 생성
        return TimeOffResponse.builder()
                .blockId(timeOff.getTrainerBlockId())
                .startAt(timeOff.getStartAt())
                .endAt(timeOff.getEndAt())
                .reason(timeOff.getReason())
                .timeOffType(timeOff.getType())
                .build();
    }

    @Transactional
    public TimeOffResponse cancelBlockTime(Long timeOffId, UserAuthInfo userAuthInfo) {
        // 1. 유저 확인
        identityQueryPort.validateTrainer(userAuthInfo.getUserId());

        // 2. time_off 된 시간조회
        TrainerTimeOff timeOff = trainerTimeOffRepository.findByTrainerBlockId(timeOffId)
                .orElseThrow(()-> new TimeOffException(TimeOffErrorCode.TIME_OFF_NOT_FOUND));

        // 3. 해당 시간 상태 바꾸기
        timeOff.cancelTimeOff(userAuthInfo.getUserId());

        // 4. 저장하기
        trainerTimeOffRepository.save(timeOff);

        // 응답 생성
        return TimeOffResponse.builder()
                .blockId(timeOff.getTrainerBlockId())
                .startAt(timeOff.getStartAt())
                .endAt(timeOff.getEndAt())
                .reason(timeOff.getReason())
                .timeOffType(timeOff.getType())
                .build();
    }

    public void validateNoTimeOffOverlap(Long trainerId,
                                         OffsetDateTime startAt,
                                         OffsetDateTime endAt) {
        boolean hasTimeOffConflict = trainerTimeOffRepository.existsConflict(trainerId, startAt, endAt);
        if (hasTimeOffConflict) {
            throw new TimeOffException(TimeOffErrorCode.TIME_OFF_CONFLICT);
        }
    }

}
