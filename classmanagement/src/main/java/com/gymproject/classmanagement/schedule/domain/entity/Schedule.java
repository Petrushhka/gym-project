package com.gymproject.classmanagement.schedule.domain.entity;

import com.gymproject.classmanagement.recurrence.domain.entity.RecurrenceGroup;
import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceStatus;
import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceType;
import com.gymproject.classmanagement.schedule.domain.event.ScheduleChangedEvent;
import com.gymproject.classmanagement.schedule.domain.policy.SchedulePolicy;
import com.gymproject.classmanagement.schedule.domain.type.ScheduleStatus;
import com.gymproject.classmanagement.schedule.exception.ScheduleErrorCode;
import com.gymproject.classmanagement.schedule.exception.ScheduleException;
import com.gymproject.classmanagement.template.domain.entity.Template;
import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import io.hypersistence.utils.hibernate.type.range.Range;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static com.gymproject.common.constant.GymTimePolicy.SERVICE_ZONE;

@Entity
@Table(name = "CLASS_SCHEDULE_TB")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Schedule extends AbstractAggregateRoot<Schedule> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_schedule_id", nullable = false)
    private Long classScheduleId;

    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_template_id", nullable = true) // null 가능(개인수업일시)
    private Template template;


    /**
     * Range<OffsetDateTime> 데이터 형식 << 이렇게하려고했는데 이거 지금 매핑을 지원안해줌(Hibernate에서)
     * Closed Range: [2025-11-26T14:57+09:00,2025-12-03T14:57+09:00]
     * Lower: 2025-11-26T14:57+09:00
     * Upper: 2025-12-03T14:57+09:00
     */
        /*
    Hibernate_types 라이브러리가 Range<OffsetDateTime> 타입을 PostgreSQL과 매핑하는 기능이 없음.
    따라서 Hibernate은 OffsetDateTimeRange를 만들 수가 없음.
    문제: private Range<OffsetDateTime> timeRange;

   해결방안은
   1. String으로 변환
   2. tsrange + LocalDateTimeRange
   (DB에는 timezone없는 timestamp 저장,
   저장 전에는 항상 UTC로 변환)

   timestamp with time zone이라도 실제로는 시간대 정보를 저장하지 않음.
   timestamptz = 입력될 때 timezone 해석 후 UTC로 바꿔 저장하는 타입

   내가 현재하던 플로우는 다음
   timezone + 시간대 -> UTC 변환 -> 저장

   문제는 UTC

     */
    @Type(PostgreSQLRangeType.class)
    @Column(name = "time_range", columnDefinition = "tstzrange")
    private Range<ZonedDateTime> timeRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ScheduleStatus status;

    @Column(name = "capacity")
    private int capacity;

    // 연속 수업(수업기간 중 특정요일)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurrence_group_id", nullable = true)
    private RecurrenceGroup recurrenceGroup;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Schedule(Long trainerId, Template template,
                     Range<ZonedDateTime> timeRange, ScheduleStatus status,
                     int capacity, RecurrenceGroup recurrenceGroup) {
        this.trainerId = trainerId;
        this.template = template;
        this.timeRange = timeRange;
        this.status = status;
        this.capacity = capacity;
        this.recurrenceGroup = recurrenceGroup;
    }


    // 0. 기본 생성자(3가지 방법으로 스케쥴이 생성됨)
    private static Schedule createSchedule(Long trainerId, Template template, RecurrenceGroup group,
                                           OffsetDateTime startAt, OffsetDateTime endAt,
                                           ScheduleStatus status, int capacity) {
        ZonedDateTime zonedStart = startAt.atZoneSameInstant(SERVICE_ZONE);
        ZonedDateTime zonedEnd = endAt.atZoneSameInstant(SERVICE_ZONE);
        Range<ZonedDateTime> range = Range.closedOpen(zonedStart, zonedEnd);

        Schedule schedule = Schedule.builder()
                .trainerId(trainerId)
                .template(template)
                .recurrenceGroup(group)
                .timeRange(range)
                .status(status)
                .capacity(capacity)
                .build();

        schedule.registerEvent(ScheduleChangedEvent.created(schedule));
        return schedule;
    }

    // 1. 연속 수업(그룹) 생성용
    public static Schedule createRecurrence(RecurrenceGroup group, Template template,
                                            OffsetDateTime startAt, OffsetDateTime endAt) {

        return createSchedule(
                group.getTrainerId(), template,
                group, startAt, endAt,
                ScheduleStatus.OPEN, template.getCapacity());
    }

    // 2. 원데이 클래스(그룹) 생성용
    public static Schedule createOneTime(Long trainerId, Template template,
                                         OffsetDateTime startAt, OffsetDateTime endAt) {
        return createSchedule(
                trainerId, template,
                null, startAt, endAt,
                ScheduleStatus.OPEN, template.getCapacity());
    }

    // 3. 1:1 수업 생성용
    public static Schedule createPersonal(Long trainerId, OffsetDateTime startAt, OffsetDateTime endAt) {
        // 개인 수업은 Template 없이, 정원 1명, 예약됨(RESERVED) 상태로 시작
        return createSchedule(
                trainerId, null,
                null, startAt, endAt,
                ScheduleStatus.RESERVED, 1);
    }

    // 4. 강제 취소 메서드(트레이너 요청)
    public void cancel(boolean isForce, int deadlineHours) {
        validateCancellationByTrainer(
                deadlineHours,
                isForce);

        this.status = ScheduleStatus.CANCELLED;

        this.registerEvent(ScheduleChangedEvent.cancelled(this));
    }

    // 5. 좌석 감소(Recurrence - Routine 용)
    public void decreaseCount(int deadlineHours) {
        validateReservation(deadlineHours);

//        RecurrenceType type = (this.recurrenceGroup != null) ?
//                this.recurrenceGroup.getRecurrenceType() : null;


        this.capacity--;

        // 이전에 정원이 다 차서 Closed 상태였다면, 자리가 생겼으므로 OPEN으로 바꿈
        // 트레이너가 강제로 폐강한 경우는 건드리지 않음
        if (this.capacity == 0 && this.status == ScheduleStatus.OPEN) {
            this.status = ScheduleStatus.CLOSED;
        }

        // 상태변경 이벤트 등록
        this.registerEvent(ScheduleChangedEvent.updated(this));
    }

    // 6. 수업 좌석 복구
    public void increaseCount() {
        // 1:1 수업(RESERVED)은 이 로직을 타지 않도록 방어 (그룹 수업 전용)
        if (this.status == ScheduleStatus.RESERVED) return;

        this.capacity++;

        // 정원이 꽉 차서 CLOSED였는데, 자리가 하나 생겼으므로 다시 OPEN으로 변경
        // 트레이너가 강제로 폐강(CANCELLED)한 경우는 건드리지 않음
        if (this.status == ScheduleStatus.CLOSED) {
            this.status = ScheduleStatus.OPEN;
        }

        // 상태 변경 이벤트 등록
        this.registerEvent(ScheduleChangedEvent.updated(this));
    }

    // 7. 상태 미러링(Recurrence - Curriculum 전용)
    public void mirrorParentStatus(int remainingCapacity,
                                   RecurrenceStatus recurrenceStatus) {

        validateMirroring();

        // 남은 인원수를 그대로 복사 붙여넣기
        this.capacity = remainingCapacity;

        // 상태 바꾸기(부모가 닫히면 나도 닫히고, 부모가 열리면 나도 열림) 부모 = Recurrence
        if (recurrenceStatus == RecurrenceStatus.CLOSED) {
            this.status = ScheduleStatus.CLOSED;
        } else if (recurrenceStatus == RecurrenceStatus.OPEN) {
            this.status = ScheduleStatus.OPEN;
        }

        // 이벤트 등록
        this.registerEvent(ScheduleChangedEvent.updated(this));
    }

    // 9. 수업 종료 (스케줄러용 메서드)
    public void finish() {
        if (this.status == ScheduleStatus.OPEN ||
                this.status == ScheduleStatus.CLOSED ||
                this.status == ScheduleStatus.RESERVED) {

            this.status = ScheduleStatus.FINISHED;

            // 종료 이벤트 발생
            this.registerEvent(ScheduleChangedEvent.finished(this));
        }
    }

    // 10. 예약 취소에 따른 수업 폐강(PERSONAL) (개인사용자)
    public void closePersonal() {
        if (this.status == ScheduleStatus.RESERVED) {
            this.status = ScheduleStatus.CANCELLED;
            this.registerEvent(ScheduleChangedEvent.cancelled(this));
        }
    }


    // ============ 헬퍼

    // 시작시간 추출
    public OffsetDateTime getStartAt() {
        if (this.timeRange == null || this.timeRange.lower() == null) {
            return null;
        }
        // ZonedDateTime을 OffsetDateTime으로 변환
        return this.timeRange.lower().toOffsetDateTime();
    }

    // 종료시간 추출
    public OffsetDateTime getEndAt() {
        if (this.timeRange == null || this.timeRange.upper() == null) {
            return null;
        }
        return this.timeRange.upper().toOffsetDateTime();
    }

    private RecurrenceType getRecurrenceType() {
        return (this.recurrenceGroup != null) ?
                this.recurrenceGroup.getRecurrenceType() : null;
    }


    // ============= validate wrappers (상태검증 + 정책검증)

    private void validateReservation(int deadlineHours) {

        if (this.status == ScheduleStatus.RESERVED) {
            throw new ScheduleException(ScheduleErrorCode.ALREADY_RESERVED_PERSONAL);
        }
        if (this.capacity <= 0) {
            throw new ScheduleException(ScheduleErrorCode.CAPACITY_EXCEEDED);
        }
        if (this.status != ScheduleStatus.OPEN) {
            throw new ScheduleException(ScheduleErrorCode.INVALID_STATUS);
        }

        // 예약 가능한 시간/타입인지
        SchedulePolicy.validateReservation(getRecurrenceType(), this.getStartAt(), deadlineHours);
    }

    private void validateCancellationByTrainer(int deadlineHours, boolean isForce) {
        if (this.status == ScheduleStatus.CANCELLED || this.status == ScheduleStatus.FINISHED) {
            throw new ScheduleException(ScheduleErrorCode.ALREADY_CLOSED_OR_FINISHED);
        }
        SchedulePolicy.validateCancellationByTrainer(this.getStartAt(), deadlineHours, isForce);
    }

    private void validateMirroring() {
        SchedulePolicy.validateMirroring(this.getStartAt());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Schedule)) return false;
        Schedule other = (Schedule) o;

        if (this.classScheduleId == null || other.classScheduleId == null) {
            return false;
        }

        return this.classScheduleId.equals(other.classScheduleId);
    }

    @Override
    public int hashCode() {
        return (classScheduleId != null) ? classScheduleId.hashCode() : 0;
    }

}
