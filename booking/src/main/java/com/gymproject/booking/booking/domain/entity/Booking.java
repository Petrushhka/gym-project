package com.gymproject.booking.booking.domain.entity;

import com.gymproject.booking.booking.application.dto.request.TicketType;
import com.gymproject.booking.booking.domain.event.BookingChangedEvent;
import com.gymproject.booking.booking.domain.policy.BookingPolicy;
import com.gymproject.booking.booking.domain.type.BookingStatus;
import com.gymproject.booking.booking.domain.type.BookingType;
import com.gymproject.booking.booking.domain.type.CancellationType;
import com.gymproject.booking.booking.exception.BookingErrorCode;
import com.gymproject.booking.booking.exception.BookingException;
import com.gymproject.common.vo.Modifier;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(name = "BOOKING_TB")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class Booking extends AbstractAggregateRoot<Booking> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "class_schedule_id", nullable = false)
    private Long classScheduleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    /// [중요] 기존에 1:1 예약을 염두해 두고 만들었다면, 현재는 그룹수업에 관한 예약도 이 엔티티에서 다룰것임.
    /// 따라서 그룹수업의 경우에는 userSessionId가 없어도 가능함.
    @Column(name = "user_session_id", nullable = true)
    private Long userSessionId;

    /// [중요] 개인 PT인지, 그룹 수업인지!
    @Enumerated(EnumType.STRING) // 없으면 숫자로 저장됨
    @Column(name = "booking_type", nullable = false)
    private BookingType bookingType;

    // 중복클릭 상황에서 동시성 제어, 낙관적 락
    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    Booking(Long classScheduleId, Long userId,
            BookingStatus status, Long sessionId,
            BookingType bookingType, OffsetDateTime createdAt) {
        this.classScheduleId = classScheduleId;
        this.userId = userId;
        this.status = status;
        this.userSessionId = sessionId;
        this.bookingType = bookingType;
        this.createdAt = createdAt;

    }

    // 1] 개인 PT 예약 생성
    public static Booking createPersonalBooking(Long scheduleId, Long userId, Long sessionId,
                                                TicketType ticketType, Modifier creator,
                                                OffsetDateTime startAt, OffsetDateTime now) {

        validateBookingDeadline(ticketType, startAt, now);
        BookingStatus initialStatus = BookingPolicy.determineInitialStatus(ticketType);

        Booking booking = Booking.builder()
                .classScheduleId(scheduleId)
                .userId(userId)
                .sessionId(sessionId)
                .status(initialStatus)
                .bookingType(BookingType.PERSONAL)
                .createdAt(now)
                .build();

        /** 해당 부분에서 @PostPersist에서 사용할 내용을 metadata로 올려놓음 */
        booking.registerEvent(BookingChangedEvent.created(booking, creator));

        return booking;
    }

    // 2] 그룹(커리큘럼/루틴) 예약 생성
    public static Booking createGroup(Long scheduleId, Long userId,
                                      BookingType bookingType,
                                      Modifier creator,
                                      OffsetDateTime now) {

        // 그룹 수업은 정책상 무조건 CONFIRMED 가정
        Booking booking = Booking.builder()
                .classScheduleId(scheduleId)
                .userId(userId)
                .sessionId(null) // 그룹은 세션 ID 관리 방식에 따라 null or 값
                .status(BookingStatus.CONFIRMED) // 그룹수업은 즉시 확정
                .bookingType(bookingType)
                .createdAt(now)
                .build();

        booking.registerEvent(BookingChangedEvent.created(booking, creator));

        return booking;
    }


    // 3] 예약 취소 (ANY -> CANCELLED)
    // 환불 정책(CancellationType)은 외부(Service/Policy)에서 계산해서 주입받음
    public CancellationType cancel(Modifier modifier, OffsetDateTime now, OffsetDateTime classStartAt, String reason) {

        checkEndedOrCancelled();

        // [핵심] 엔티티가 직접 Static Policy를 사용하여 취소 타입을 결정합니다.
        CancellationType type = BookingPolicy.calculateCancellationType(
                this.getCreatedAt(), // 예약 생성 시간 (엔티티 필드)
                now,                 // 현재 시간
                classStartAt         // 수업 시작 시간 (외부에서 주입)
        );

        validateCancelable(type);

        this.status = BookingStatus.CANCELLED;

        // 이벤트
        this.registerEvent(BookingChangedEvent.cancelled(this, modifier, type, reason));

        return type;
    }

    // 4] 커리큘럼형 수업들 중 첫번째 수업만 검증하여 예약취소
    public void cancelFirstSchedule(Modifier modifier,
                                    OffsetDateTime classStartAt,
                                    OffsetDateTime now,
                                    String reason) {

        BookingPolicy.validateCurriculumCancellation(classStartAt, now);

        this.status = BookingStatus.CANCELLED;

        this.registerEvent(BookingChangedEvent.cancelled(this, modifier,
                CancellationType.FREE_CANCEL, reason));

    }

    // 5] 검증없이 모두 한번에 취소
    public void cancelWithoutValidation(Modifier modifier, String reason) {
        this.status = BookingStatus.CANCELLED;

        this.registerEvent(BookingChangedEvent.cancelled(this, modifier,
                CancellationType.FREE_CANCEL, reason));
    }


    // ========== 상태 변경 및 이벤트 발행

    // 1. 예약 승인 (Pending -> Confirmed)
    public void confirm(Modifier modifier) {
        if (this.status != BookingStatus.PENDING) {
            throw new BookingException(BookingErrorCode.NOT_PENDING_STATUS);
        }

        this.status = BookingStatus.CONFIRMED;
        this.registerEvent(BookingChangedEvent.approved(this, modifier));
    }

    // 2. 예약 거절 (PENDING -> REJECTED)
    public void reject(Modifier modifier, String reason) {
        if (this.status != BookingStatus.PENDING) {
            throw new BookingException(BookingErrorCode.NOT_PENDING_STATUS);
        }
        this.status = BookingStatus.REJECTED;
        this.registerEvent(BookingChangedEvent.rejected(this, modifier, reason));
    }

    // 3. 출석 처리 (CONFIRMED -> ATTENDED)
    public void attend(Modifier modifier, OffsetDateTime classStartAt,
                       OffsetDateTime now, double distance) {
        if (this.status != BookingStatus.CONFIRMED) {
            throw new BookingException(BookingErrorCode.NOT_CONFIRMED_STATUS);
        }

        BookingPolicy.validateCheckInTime(classStartAt, now);
        BookingPolicy.validateCheckInDistance(distance);

        this.status = BookingStatus.ATTENDED;
        this.registerEvent(BookingChangedEvent.attended(this, modifier));
    }

    // 4. 노쇼 처리 (CONFIRMED -> NOSHOW)
    public void noShow(Modifier modifier, String reason) {
        if (this.status != BookingStatus.CONFIRMED) {
            throw new BookingException(BookingErrorCode.NOT_CONFIRMED_STATUS);
        }
        this.status = BookingStatus.NOSHOW;
        this.registerEvent(BookingChangedEvent.noShow(this, modifier, reason));
    }


    // ======== 검증 및 헬퍼


    // 이미 예약상태가 종료됐는지 확인
    public void checkEndedOrCancelled() {
        if (this.status == BookingStatus.CANCELLED
                || this.status == BookingStatus.REJECTED
                || this.status == BookingStatus.NOSHOW
                || this.status == BookingStatus.ATTENDED) {
            throw new BookingException(BookingErrorCode.ALREADY_CANCELLED);
        }
    }

    private static void validateBookingDeadline(TicketType ticketType, OffsetDateTime startAt, OffsetDateTime now) {
        BookingPolicy.validateBookingDeadline(ticketType, startAt, now);
    }

    private static void validateCancelable(CancellationType type) {
        if (type == CancellationType.IMPOSSIBLE) {
            throw new BookingException(BookingErrorCode.CANCELLATION_NOT_ALLOWED);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Booking)) return false;

        Booking other = (Booking) o;

        // 둘 다 영속상태가 아닐 때(PK 없음)는 같은 엔티티로 취급하면 안 됨
        if (this.bookingId == null || other.bookingId == null) {
            return false;
        }

        return this.bookingId.equals(other.bookingId);
    }

    @Override
    public int hashCode() {
        // JPA 엔티티는 PK가 assign 전/후로 hashCode가 바뀌면 안 됨 (영속성 컨텍스트 버그 방지)
        return 31;
    }


    /**
     * 31로 하는 이유
     * 관례임. 31은 홀수이면서 소수여서 hash 충돌을 조금 줄여줌.
     * Effective Java에서도 31 사용을 권장한 전통이 있다고함.
     * 아무거나 사용해도 상관은없음.
     */

}


/**
 * JPA는 DB에 바로 저장하지 않음
 * 영속성 컨텍스트라는 메모리에 저장을 해두고 필요할 때 flush를 통해 DB에 반영.
 * <p>
 * save()가 DB INSERT를 의미하지 않는다는 것임.
 * <p>
 * flush()를 해야 DB에 반영이 됨.
 * <p>
 * flush()는 언제 발생하냐?
 * -> 트랜잭션 commit 직전
 * -> JPQL/ QueryDSL 실행 직전
 * -> 명시적으로 em.flush()를 호출할 경우
 * <p>
 * save()는 persist(영속화)만 하고,
 * flush를 해야 INSERT SQL이 실행됨.
 *
 * @PostPersist의 실행타이밍은 다음과 같음
 * <p>
 * flush() -> DB INSERT -> PK 할당 -> @PostPersist 실행(이벤트를 이때 실행하는것임)
 * <p>
 * Listener의 경우
 * @TransactionalEventListener(phase = AFTER_COMMIT)
 * <p>
 * History의 INSERT는 예약이 DB에 commit이 성공했을 때만 실행하겠다는 의미;
 * <p>
 * save() 호출
 * ↓
 * persist() → 영속성 컨텍스트 등록
 * ↓
 * flush() (commit 직전)
 * ↓
 * INSERT SQL 실행
 * ↓
 * DB에서 PK 생성 및 Booking에 주입
 * ↓
 * @PostPersist 실행(★ 이벤트 등록)
 * ↓
 * commit
 * ↓
 * @TransactionalEventListener(AFTER_COMMIT) 실행
 * ↓
 * BookingHistory 저장
 */

/**
 * AbstractAggregateRoot.registerEvent()
 * <p>
 * Spring Data JPA에서 제공하는 DDD스타일 도메인 이벤트 처리기임.
 * <p>
 * AbstractAggregateRoot 클래스가 List<Object>를 가지고 있음.
 * <p>
 * 해당 리스트에 event 객체를 등록.
 * <p>
 * Spring data repository가 save()를 실행하면, @DomainEvents 메서드를 실행하여 이벤트 목록을 추출함.
 * ApplicationEventPublisher.publishEvent()로 이벤트 발행,
 * 후에 이벤트 발행이 끝나면 @AfterDomainEventPublication 실행 하여 리스트를 초기화 시킴.
 * <p>
 * 내가 겪어던 문제는 다음과 같음
 * <p>
 * save() -> flush 미실행 -> event 발행 -> bookingId == null
 *
 * @PostPersist에서 registerEvent를 하여 해결하였음. 해당 내용은 아래있음.
 */