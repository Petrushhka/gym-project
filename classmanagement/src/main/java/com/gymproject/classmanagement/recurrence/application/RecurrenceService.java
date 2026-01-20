package com.gymproject.classmanagement.recurrence.application;

import com.gymproject.classmanagement.recurrence.application.dto.RecurrenceClassClassRequest;
import com.gymproject.classmanagement.recurrence.application.dto.OneTimeClassResponse;
import com.gymproject.classmanagement.recurrence.application.dto.RecurrenceResponse;
import com.gymproject.classmanagement.recurrence.application.dto.OneTimeClassRequest;
import com.gymproject.classmanagement.recurrence.domain.entity.RecurrenceGroup;
import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceType;
import com.gymproject.classmanagement.recurrence.exception.RecurrenceErrorCode;
import com.gymproject.classmanagement.recurrence.exception.RecurrenceException;
import com.gymproject.classmanagement.recurrence.infrastructure.persistence.RecurrenceGroupRepository;
import com.gymproject.classmanagement.schedule.domain.service.ScheduleValidator;
import com.gymproject.classmanagement.schedule.exception.ScheduleErrorCode;
import com.gymproject.classmanagement.schedule.exception.ScheduleException;
import com.gymproject.classmanagement.template.domain.entity.Template;
import com.gymproject.classmanagement.template.infrastructure.persistence.TemplateReader;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.port.auth.IdentityQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import static com.gymproject.common.constant.GymTimePolicy.SERVICE_ZONE;

@Service
@RequiredArgsConstructor
@Transactional
public class RecurrenceService {

    /*[중요]
        해당 도구들을 ScheduleService에 몰아넣고 거기서 메서드를 꺼내쓰려고 했지만,
        추후 순환참조에 걸릴 것 같아서 도구를 많이 넣음.
        ScheduleService가 나중에 RecurrenceService를 부를 일이 단 1%로 없으면
        주입받아 사용해도 괜찮지만 그렇지 않을일이 더 많음.
        그럼 UserMembershipService는? 방향 자체가 USerMembership -> USer라서 상관없었던 것
        Schedule의 경우 그룹수업 취소할때 Recurrence를 다시 바라볼 일이 생김.
     */
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final ScheduleValidator scheduleValidator;
    private final TemplateReader templateReader;
    private final IdentityQueryPort identityQueryPort;

    // 1] 기간제수업 생성(커리큘럼, 루틴형 둘다)
    @Transactional
    public RecurrenceResponse openRecurrenceClass(RecurrenceClassClassRequest requestDto, UserAuthInfo userAuthInfo) {
        // 1. 권한 검증
        identityQueryPort.validateTrainer(userAuthInfo.getUserId());
        Long trainerId = userAuthInfo.getUserId();

        // 2. 템플릿 조회
        Template template = templateReader.getTemplate(requestDto.getClassTemplateId());

        // 3. 반복 규칙(Group) 저장
        RecurrenceGroup group = RecurrenceGroup.create(
                trainerId,
                template,
                requestDto.getStartDate(),
                requestDto.getEndDate(),
                requestDto.getStartTime(),
                requestDto.getRepeatDays(),
                requestDto.getTimezoneId(),
                requestDto.getRecurrenceType());

        recurrenceGroupRepository.save(group);

        // 응답 생성
        return RecurrenceResponse.builder()
                .recurrenceId(group.getGroupId())
                .title(group.getTemplate().getTitle())
                .startDate(group.getStartDate())
                .endDate(group.getEndDate())
                .recurrenceType(group.getRecurrenceType())
                .build();
    }

    // 2] 커리큘럼형 수업 폐강
    @Transactional
    public RecurrenceResponse cancelRecurrenceClass(Long recurrenceId, UserAuthInfo userAuthInfo) {
        // 1. 유저 검증
        identityQueryPort.validateTrainer(userAuthInfo.getUserId());

        // 2. 본인 수업인지 확인
        RecurrenceGroup recurrenceGroup = getRecurrenceGroup(recurrenceId);
        validateOwner(recurrenceGroup, userAuthInfo.getUserId());

        // 3. 수업 취소
        recurrenceGroup.cancelRecurrenceClass();

        recurrenceGroupRepository.save(recurrenceGroup); // << 이부분이 없어서 이벤트가 작동이 안됨. 왜 여기는 Dirty check 대상이 아닐까?, Domain Events는 Spring Data의 기능이라 그럼.
        /* [중요]
            Dirty Checking: 트랜잭션이 끝날 때 엔티티의 상태가 변했는지 감시하다가(감시 자체는 Persistence Context(영속성 컨텍스트가 함),
              엔티티 최초 로딩시 스냅샷, flush 시점에서 현재 상태와 스냅샷 비교, 이벤트,도메인로직 등은 모름
             변했으면 Update SQL을 날려줌
            소속: JPA(hibernate)
            특징: 객체의 필드 값이 바뀐 것만 관심있음. 이벤트가 쌓여있는지에 대해서는 관심없음.

            Domain Events(save) 이벤트 전파의 트리거
            registerEvent()로 쌓아둔 이벤트리스트를 꺼내서 스프링 ApplicationEventPublisher에게 전달
            소속: Spring Data JPA
            특징: 이벤트를 꺼내서 이벤트를 전달하는 행위가 save() 메서드 안에 구현되어있음.

            [중요한 부분]!!!
            Dirty Checking과 Domain Event 발행은 연결되어 있지 않음.
            따라서 save()를 호출해야만 Spring Data JPA가 entity.domainEvents()를 꺼내고,
            ApplicationEventPublisher.publishEvent()를 호출하게됨.

            Dirty Checking만으로는 절대 이벤트가 안나간다!
         */

        return RecurrenceResponse.builder()
                .title(recurrenceGroup.getTemplate().getTitle())
                .recurrenceId(recurrenceGroup.getGroupId())
                .startDate(recurrenceGroup.getStartDate())
                .endDate(recurrenceGroup.getEndDate())
                .recurrenceType(recurrenceGroup.getRecurrenceType())
                .totalCreatedCount(recurrenceGroup.getTemplate().getCapacity())
                .build();

    }

    // 3] 하루 그룹 수업 개설(반복요일없음)
    @Transactional
    public OneTimeClassResponse openOneTimeGroupClass(OneTimeClassRequest request, UserAuthInfo userAuthInfo) {
        // 1. 권한 확인
        identityQueryPort.validateTrainer(userAuthInfo.getUserId());
        Long trainerId = userAuthInfo.getUserId();

        // 2. 템플릿 확인
        Template template = templateReader.getTemplate(request.getClassTemplateId());

        // 3. 기준 시간 (UTC) 맞추기
        // 시작 시간
        OffsetDateTime startAt = request.getStartAt();
        // 종료시간
        OffsetDateTime endAt = request.getEndAt(template.getDurationMinutes());

        // 수업 시간 충돌 검증
        scheduleValidator.validateConflict(trainerId, startAt, endAt);

        // 그룹 생성
        RecurrenceGroup group = RecurrenceGroup.create(
                trainerId,
                template,
                startAt.toLocalDate(),
                endAt.toLocalDate(),
                request.getStartTime(),
                List.of(request.getStartDate().getDayOfWeek()),
                request.getTimezoneId(),
                RecurrenceType.ROUTINE
        );

        recurrenceGroupRepository.save(group);

        // 응답 생성

        OffsetDateTime responseStartAt = ZonedDateTime.of(
                        group.getStartDate(),
                        group.getStartTime(),
                        SERVICE_ZONE).toOffsetDateTime();

        return OneTimeClassResponse.builder()
                .title(group.getTemplate().getTitle())
                .scheduleId(group.getGroupId())
                .startAt(responseStartAt)
                .endAt(responseStartAt.plusMinutes(group.getTemplate().getDurationMinutes()))
                .capacity(group.getRemainingCapacity())
                .status(group.getRecurrenceStatus().name())
                .build();

        /** 이전버전
         파라미터용으로 먼저 만들고 추후 완성해서 저장
         Schedule.ScheduleBuilder builder = Schedule.builder()
         .startAt(startAt)
         .endAt(endAt);
         Schedule tempSchedule = builder.build();

         validateConflict(trainerId, List.of(tempSchedule));

         Schedule schedule = builder
         .trainerId(trainerId)
         .template(template)
         .status(ScheduleStatus.OPEN)
         .capacity(template.getCapacity())
         .build();

         */
    }

    // 4] 커리큘럼 예약 처리(그룹 잔여석 차감)
    @Transactional
    public void reserveCurriculum(Long recurrenceGroupId) {
        /// [중요] 1. 그룹 조회 (Lock 처리)
        RecurrenceGroup group = recurrenceGroupRepository.findByIdWithLock(recurrenceGroupId)
                .orElseThrow(() -> new ScheduleException(ScheduleErrorCode.NOT_FOUND,
                        "커리큘럼 그룹을 찾을 수 없습니다. ID: " + recurrenceGroupId));


        // 2. 그룹 잔여석 차감 (Domain 로직)
        group.reserveCurriculum();

        recurrenceGroupRepository.save(group);
    }

    // 5] 커리큘럼 예약 취소(그룹 잔여석 복구)
    @Transactional
    public void cancelCurriculumReservation(Long recurrenceGroupId){
        // 1. 그룹 조회
        RecurrenceGroup group = recurrenceGroupRepository.findById(recurrenceGroupId)
                .orElseThrow(()-> new RecurrenceException(RecurrenceErrorCode.NOT_FOUND));

        // 2. 그룹 상태 변경(좌석 복구)
        group.cancelCurriculumReservation();

        recurrenceGroupRepository.save(group);
    }

    private RecurrenceGroup getRecurrenceGroup(Long recurrenceId) {
        RecurrenceGroup recurrenceGroup =
                recurrenceGroupRepository.findById(recurrenceId)
                        .orElseThrow(() -> new RuntimeException("Recurrence group not found"));
        return recurrenceGroup;
    }

    private static void validateOwner(RecurrenceGroup recurrenceGroup,  Long userId) {
        if(recurrenceGroup.getTrainerId() != userId) {
            throw new RecurrenceException(RecurrenceErrorCode.ACCESS_DENIED);
        }
    }
}

