package com.gymproject.booking.timeoff.domain.entity;

import com.gymproject.booking.timeoff.domain.event.TimeOffChangedEvent;
import com.gymproject.booking.timeoff.domain.policy.TrainerTimeOffPolicy;
import com.gymproject.booking.timeoff.domain.type.TimeOffStatus;
import com.gymproject.booking.timeoff.domain.type.TimeOffType;
import com.gymproject.common.util.GymDateUtil;
import io.hypersistence.utils.hibernate.type.range.PostgreSQLRangeType;
import io.hypersistence.utils.hibernate.type.range.Range;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "TRAINER_TIME_OFF_TB")
public class TrainerTimeOff extends AbstractAggregateRoot<TrainerTimeOff> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trainer_block_id", nullable = false)
    private Long trainerBlockId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "time_range", columnDefinition = "tstzrange")
    private Range<ZonedDateTime> timeRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TimeOffType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TimeOffStatus status;

    @Column(name = "reason")
    private String reason;

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
    TrainerTimeOff(Long userId,
                   Range<ZonedDateTime> timeRange,
                   TimeOffType type,
                   TimeOffStatus status,
                   String reason) {
        this.userId = userId;
        this.timeRange = timeRange;
        this.type = type;
        this.status = status;
        this.reason = reason;
    }

    // 생성 메서드
    public static TrainerTimeOff create(Long userId,
                                        OffsetDateTime startAt,
                                        OffsetDateTime endAt,
                                        TimeOffType type,
                                        String reason) {


        Range<ZonedDateTime> timeRange = TrainerTimeOffPolicy.createTimeRange(startAt, endAt);

        TrainerTimeOff timeOff = TrainerTimeOff.builder()
                .userId(userId)
                .timeRange(timeRange)
                .type(type)
                .status(TimeOffStatus.REGISTERED)
                .reason(reason)
                .build();

        timeOff.registerEvent(TimeOffChangedEvent.created(timeOff));

        return timeOff;
    }

    /**
     * 왜 생성에는 @PostPersist를 사용했는데,
     * 취소시에는 사용하지 않는지?
     * @PostUpdate를 사용하면 트랜잭션이 끝나고, flush가 일어난 직후에 실행됨,
     * 하지만 AbstractAggregateRoot의 registerEvent는 save()가 호출되는 시점에 이벤트가 수집되어야 스프링이 이를 낚아채서 발행해줌.
     *
     * @param requestId
     */
    //  차단시간 취소
    public void cancelTimeOff(Long requestId) {
        // 1. 본인의 휴가가 맞는지 검증
        ownerValid(requestId);
        // 2. 이미 취소된 건지 확인
        validateCancelled();

        // 3. 상태 변경
        this.status = TimeOffStatus.CANCELLED;

        // 4. 이벤트 발행
        this.registerEvent(TimeOffChangedEvent.cancelled(this));
    }

    public OffsetDateTime getStartAt() {
        return this.timeRange.lower().toOffsetDateTime();
    }

    public OffsetDateTime getEndAt() {
        return this.timeRange.upper().toOffsetDateTime();
    }

    // 본인의 휴가 맞는지?
    private void ownerValid(Long requestId) {
        TrainerTimeOffPolicy.validateOwner(this.userId, requestId);
    }

    private void validateCancelled() {
        TrainerTimeOffPolicy.validateCancellation(this.status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrainerTimeOff that = (TrainerTimeOff) o;

        if (this.trainerBlockId != null && that.trainerBlockId != null) {
            return this.trainerBlockId.equals(that.trainerBlockId);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 31;
    }

}


/***
 * tstzragne의 경우는 잘안보이는 이유가 있음
 * 그래도 예약/일정 시스템에서는 정말 유용한 데이터타입임
 * mysql에서는 해당 데이터타입이 없기때문에 사용을 못하는것뿐.
 *
 * 다만 JPA에서 기본 지원을 안하는게 문제임.
 * 표준 JPA 모든 DB에서 돌아가야하므로 특정 DB에만 있는 기본 기능을 넣지는 않음.
 * 따라서 hiberNate-types 같은 외부라이브러리를 써야하는 번거로움이 생김.
 *
 * <안쓰는 이유>
 * 번거롭거나, 몰라서 안쓰는거임.

 <Postgre 에서는 거의 표준>
 쿼리가 매우매우 간단해짐.
 DB레벨에서 중복 예약 원천 봉쇄(겹치는 시간을 저장하지 못하게 제약 조건이 기본적으로 걸림)
 *
 */

/**
 * Range<OffsetDateTime>을 사용하려고했지만 매핑을 지원해주는 라이브러리가 없음
 * Range<ZoneDateTime>으로 변경함.
 * <p>
 * OffsetDatetime보다 ZoneDateTime이 담고있는 정보량이 더 많아서임
 * <p>
 * OffsetDateTime의 경우 UTC기준 +9 (서울, 도쿄 전부같음) << 이정도의 정보만 가지고 있지만
 * ZoneDateTime은 UTC기준 +9, Asia/Seoul << zoneId까지 정보를 다룸.
 */
