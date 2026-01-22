package com.gymproject.classmanagement.recurrence.domain.entity;

import com.gymproject.classmanagement.recurrence.domain.event.RecurrenceGroupEvent;
import com.gymproject.classmanagement.recurrence.domain.policy.RecurrencePolicy;
import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceStatus;
import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceType;
import com.gymproject.classmanagement.recurrence.exception.RecurrenceErrorCode;
import com.gymproject.classmanagement.recurrence.exception.RecurrenceException;
import com.gymproject.classmanagement.template.domain.entity.Template;
import com.gymproject.common.util.GymDateUtil;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "CLASS_RECURRENCE_GROUP_TB")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurrenceGroup extends AbstractAggregateRoot<RecurrenceGroup> {

    public static Logger log = LoggerFactory.getLogger(RecurrenceGroup.class);
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    // 트레이너 (User 테이블 참조)
    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    // 템플릿 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_template_id", nullable = false)
    private Template template;

    // 기간은 날짜만 있으면 됨(Timezone 불필요)
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /* 반복 요일 배열
        ["MONDAY", "WEDNESDAY", "FRIDAY"] 의 데이터를 저장하려면?
        1) String으로 저장 -> 검색이 불편
        2) 보조 테이블을 만들어 저장 -> 불필요한 조인
        3) text[] 검색쉽고, 저장 편함, 공간 효율, 복잡한 조인 없음

        JPA는 Text[]를 처리할 수 없어서 Converter가 필요
     */
    @Column(name = "repeat_days", columnDefinition = "jsonb")
    @Convert(converter = DayOfWeekListConverter.class)
    private List<DayOfWeek> repeatDays;

    // 시작 시간
    // 반복되는 시각만 필요하기에 OffsetDateTIme 불필요
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    // 규칙이 적용되는 기준 시간대
    @Column(name = "time_zone_id", nullable = false)
    private String timezoneId;

    // 동일루틴형 수업 -> 가능 || 과정형 수헙 -> 불가능
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false)
    private RecurrenceType recurrenceType;

    // 몇좌석이 남아 있는지?
    @Column(name = "remaining_capacity")
    private int remainingCapacity;

    // 해당 수업 전체가 활성화 되어있는지?
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RecurrenceStatus recurrenceStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // 호주시간으로 저장
    @PrePersist
    public void onPrePersist() {
        OffsetDateTime now = GymDateUtil.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // 호주시간으로 저장
    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = GymDateUtil.now();
    }

    @Builder(access = AccessLevel.PRIVATE)
    private RecurrenceGroup(Long trainerId,
                            Template template,
                            LocalDate startDate,
                            LocalDate endDate,
                            List<DayOfWeek> repeatDays,
                            LocalTime startTime,
                            String timezoneId,
                            RecurrenceType recurrenceType,
                            int remainingCapacity,
                            RecurrenceStatus recurrenceStatus) {

        this.trainerId = trainerId;
        this.template = template;
        this.startDate = startDate;
        this.endDate = endDate;
        this.repeatDays = repeatDays;
        this.startTime = startTime;
        this.timezoneId = timezoneId;
        this.recurrenceType = recurrenceType;
        this.remainingCapacity = remainingCapacity;
        this.recurrenceStatus = recurrenceStatus;
    }

    // 1. 강좌 그룹 생성
    public static RecurrenceGroup create(Long trainerId, Template template,
                                         LocalDate startDate, LocalDate endDate,
                                         LocalTime startTime, List<DayOfWeek> repeatDays,
                                         String timezoneId, RecurrenceType recurrenceType) {

        validateCreate(startDate, endDate, repeatDays);

        RecurrenceGroup group=  RecurrenceGroup.builder()
                .trainerId(trainerId)
                .template(template)
                .startDate(startDate)
                .endDate(endDate)
                .repeatDays(repeatDays)
                .startTime(startTime)
                .timezoneId(timezoneId)
                .recurrenceType(recurrenceType)
                .remainingCapacity(template.getCapacity())
                .recurrenceStatus(RecurrenceStatus.OPEN)
                .build();

        group.registerEvent(RecurrenceGroupEvent.created(group));

        return group;
    }

    // 2. 프로그램형 수업 예약(전체 회차권)
    public void reserveCurriculum() {
        validateProgramReservation();

        this.remainingCapacity--;

        if (this.remainingCapacity == 0) {
            this.recurrenceStatus = RecurrenceStatus.CLOSED;
        }
        ///  [중요] 트랜잭션으로 묶여야하는 중요한 로직이지만 @EventListener 역시도 동기적으로 작동하여 상관없음.
        // Event 발생하여 Schedule 쪽으로 넘김
        this.registerEvent(RecurrenceGroupEvent.updated(this)
        );
    }

    // 3. 프로그램형 수업 예약 취소
    public void cancelCurriculumReservation() {
        validateCancellation();
        checkCapacity();

        // 1. 좌석 수 채우기
        this.remainingCapacity++;

        // 2. 남은 좌석이 0 이상이라면 상태를 OPEN으로 변경
        if (this.remainingCapacity > 0 && this.recurrenceStatus == RecurrenceStatus.CLOSED) {
            this.recurrenceStatus = RecurrenceStatus.OPEN;
        }

        // Event 발생하여 Schedule 쪽으로 넘김
        this.registerEvent(RecurrenceGroupEvent.updated(this)
        );
    }

    // 4. 트레이너가 전체수업을 취소
    public void cancelRecurrenceClass() {
        validateCancellation();

        // 2. 상태 변경
        this.recurrenceStatus = RecurrenceStatus.CANCELLED;

        // 3. 도메인 이벤트 발생
        this.registerEvent(RecurrenceGroupEvent.cancelled(this));

    }

    //  5. 기간 종료에 따른 상태 마감
    public void finish() {
        // 취소 빼고는 전부 종료 처리
        if (this.recurrenceStatus == RecurrenceStatus.OPEN || this.recurrenceStatus == RecurrenceStatus.CLOSED) {
            this.recurrenceStatus = RecurrenceStatus.FINISHED;
        }
    }


    //  === 검증

    private static void validateCreate(LocalDate startDate, LocalDate endDate, List<DayOfWeek> repeatDays) {
        RecurrencePolicy.validateCreate(startDate, endDate, repeatDays);
    }

    private void validateProgramReservation() {
        if (this.recurrenceStatus != RecurrenceStatus.OPEN) {
            throw new RecurrenceException(RecurrenceErrorCode.INVALID_STATUS);
        }

        if (remainingCapacity <= 0) {
            throw new RecurrenceException(RecurrenceErrorCode.CAPACITY_EXCEEDED);
        }
        RecurrencePolicy.validateProgramReservation(recurrenceType, startDate);
    }

    private void validateCancellation() {
        if (this.recurrenceStatus == RecurrenceStatus.FINISHED ||
                this.recurrenceStatus == RecurrenceStatus.CANCELLED) {
            throw new RecurrenceException(RecurrenceErrorCode.ALREADY_CLOSED_OR_FINISHED);
        }
        RecurrencePolicy.validateCancellation(startDate);
    }

    private void checkCapacity() {
        if (this.remainingCapacity >= this.template.getCapacity()) {
            throw new RecurrenceException(RecurrenceErrorCode.CANCEL_CAPACITY_ERROR);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecurrenceGroup that = (RecurrenceGroup) o;
        return this.groupId != null && this.groupId.equals(that.groupId);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

/*
    [중요]
    Wall Clock 의 법칙(벽시계의 법칙)
    트레이너가 매주 월요일 10시에 수업을 만들었다면

    트레이너의 생각: 서머타임이 오든 말든, 내 눈앞에 시계가 10시일 때 수업을 시작한다!

    11시로 밀리면 안됨!

    서머 타임전인 9월에 10시를 UTC+10 기준으로 저장하고
    서머 타임 후에는 10시를 UTC+11 기준으로 저장함

    결과적으로 사용자는 둘 다 오전 10:00 라고 찍힘.

    LocalDateTime으로 저장하면 안되나??
    - 어느 지역의 10시인지를 모름.
    - 문제는 서버는 보통 UTC(영국) 또는 서버위치의 시간으로 돌아감.
    - 나중에 시드니의 10시인지 서울의 10시인지 알 길이 없음.

    OffsetDateTime: 절대적인 위치
    물리적인 시간이 바뀜.

    근데 벽시계의 법칙에 의하면 LocalDateTime으로 무조건 10시라는 개념만 저장하면 되는거 아닌가?

    LocalDateTime의 문제점
    9월 29일 수업: 오전 10시
    10월 6일 수업: 오전 10시

    우리 생각: 똑같이 0시 기준에서 10시간 차이나는 시점이겠지?(잘못된 생각)

    실제로는
    9월 29일 10시 -> UTC로 00:00(영국은 시드니보다 10시간 느림)
    10월 6일은 10시 -> UTC로 23:00 (서머타임 때문에 더 느림)

    OffsetDateTime은 물리적인 사실 기반(LocalDateTime + ZonedOffset)
    ZonedDateTime은 규칙까지 알고 있음(LocalDateTime + ZonedId)

    DB에 저장할 때는 OffsetDateTime
    계산할때는 ZonedDateTime

    서머타임이 적용되면 오프셋이 변하니까, 그 변화를 '계산'하기 위해서 ZonedDateTime이 꼭 필요한 것이고, 그 '결과'를 DB에 안전하게 '좌표'로 찍기 위해 OffsetDateTime을 쓰는 것.
 */
