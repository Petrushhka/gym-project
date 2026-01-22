package com.gymproject.user.sesssion.domain.entity;

import com.gymproject.common.util.GymDateUtil;
import com.gymproject.common.vo.Modifier;
import com.gymproject.user.profile.domain.entity.User;
import com.gymproject.user.profile.domain.type.UserSessionStatus;
import com.gymproject.user.sesssion.domain.event.SessionChangedEvent;
import com.gymproject.user.sesssion.domain.policy.SessionPolicy;
import com.gymproject.user.sesssion.domain.type.SessionProductType;
import com.gymproject.user.sesssion.domain.type.SessionType;
import com.gymproject.user.sesssion.exception.UserSessionErrorCode;
import com.gymproject.user.sesssion.exception.UserSessionsException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.OffsetDateTime;

@Entity
@Table(name = "USER_SESSION_TB")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSession extends AbstractAggregateRoot<UserSession> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_sessions", nullable = false)
    private int totalSessions;

    @Column(name = "used_sessions", nullable = false)
    private int usedSessions = 0;

    @Column(name = "start_at")
    private OffsetDateTime startAt;

    @Column(name = "expire_at")
    private OffsetDateTime expireAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserSessionStatus status;

    // 해당 컬럼이 없어도, 처음 가입시에 세션을 1만큼 지급하면 되지만,
    // 무료체험말고 돈을 내고 세션권을 구매한 사람을 조회하기 위해 컬럼을 추가한 것
    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_product_type", nullable = false)
    private SessionProductType sessionProductType;

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

    @Version
    @Column(name = "version", nullable = false)
    private Long version; // [동시 예약을 막기위한 장치]

    @Builder(access = AccessLevel.PRIVATE)
    public UserSession(User user,
                       int totalSessions,
                       int usedSessions,
                       OffsetDateTime startAt,
                       OffsetDateTime expireAt,
                       UserSessionStatus status,
                       SessionType sessionType,
                       SessionProductType sessionProductType) {

        this.user = user;
        this.totalSessions = totalSessions;
        this.usedSessions = usedSessions != 0 ? usedSessions : 0;
        this.startAt = startAt;
        this.expireAt = expireAt;
        this.status = status != null ? status : UserSessionStatus.ACTIVE;
        this.sessionType = sessionType;
        this.sessionProductType = sessionProductType;
    }

    // 1. 무료 세션 생성(비니지스 정책상 30일까지만 사용가능)
    public static UserSession createFreeTrial(User user, Modifier modifier, OffsetDateTime startAt) {
        validateNullUser(user);

        UserSession session = UserSession.builder()
                .user(user)
                .totalSessions(1)
                .usedSessions(0)
                .sessionType(SessionType.FREE_TRIAL) // 무료타입
                .expireAt(SessionPolicy.calculateFreeTrialExpiredAt(startAt))// 30일 안으로 무료체험 가능
                .status(UserSessionStatus.ACTIVE)
                .sessionProductType(SessionProductType.FREE)
                .build();

        session.registerEvent( // 초기 무료 세션티켓: 횟수 1번 지급
                SessionChangedEvent.issued(session, modifier, 1)
        );

        return session;
    }

    // 2. 유료 세션 결제시 유료 세션 생성 메서드
    public static UserSession createPaid(User user, SessionProductType productType, Modifier modifier,
                                       OffsetDateTime startDate  ,OffsetDateTime endDate) {
        validateNullUser(user);
        validateDateRange(startDate, endDate);

        UserSession session = UserSession.builder()
                .user(user)
                .totalSessions(productType.getSessionCount())
                .sessionProductType(productType)
                .usedSessions(0)
                .sessionType(SessionType.PAID) // 유료타입
                .status(UserSessionStatus.ACTIVE)
                .startAt(startDate)
                .expireAt(endDate)
                .build();

        // 생성 직후 이벤트를 등록
        session.registerEvent(SessionChangedEvent.purchased(session, modifier, session.totalSessions));

        return session;
    }

    private static void validateDateRange(OffsetDateTime startDate, OffsetDateTime endDate) {
        // 1. 필수값 체크
        if (startDate == null || endDate == null) {
            throw new UserSessionsException(UserSessionErrorCode.INVALID_DATE_FORMAT);
        }

        // 2. 논리 오류 체크 (종료일이 시작일보다 앞섬)
        if (endDate.isBefore(startDate)) {
            throw new UserSessionsException(UserSessionErrorCode.INVALID_DATE_RANGE);
        }

    }

    // 3. 세션 사용
    public void use(Modifier modifier, OffsetDateTime now) {
        // 1) 사용할 수 있는 세션인지 검증
        validateForUse(now);

        // 2) 비지니스 로직 수행
        this.usedSessions++;

        // 3) 상태 변경
        if (this.usedSessions >= this.totalSessions) {
            this.status = UserSessionStatus.FULLY_USED;
        }

        // 4) 이벤트 발행
        this.registerEvent(SessionChangedEvent.used(this, modifier));
    }

    // 4. 세션 복구 (예약 취소시)
    public void restore(Modifier modifier) {
        validateForRestore();

        this.usedSessions--;

        // 사용량이 전체보다 적어지면 다시 ACTIVE 상태로 전환
        // 만약에 이 사이에 세션권의 유효기간이 지나간다면? -> use시점에서 자동으로 validate되므로 냅둠.
        if (this.usedSessions < this.totalSessions
                && this.status == UserSessionStatus.FULLY_USED) {
            this.status = UserSessionStatus.ACTIVE;
        }

        this.registerEvent(SessionChangedEvent.restored(this, modifier));
    }
    /*
        만료기간이 지난 것도 Fully-uses -> Active로 넘어올수 있지만
        사용하려고하면 사용로직에서 차단이 걸림
     */

    // 5. 만료 처리
    public void expire(Modifier modifier) {
        validateExpired();
        //history 용 (snapshot)
        int expiredAmount = this.getRemainingSessions();
        this.status = UserSessionStatus.EXPIRED;
        this.registerEvent(SessionChangedEvent.expired(this, modifier, -expiredAmount));
    }

    // 6. 사용 정지
    public void deactivate(Modifier modifier) {
        validateDeactivated();
        this.status = UserSessionStatus.DEACTIVATED;
        this.registerEvent(SessionChangedEvent.deactivated(this, modifier));
    }

    // 7. 환불 처리
    public void refund(Modifier modifier, OffsetDateTime now) {
        validateRefund(now);
        // snapshot 용
        // 환불 전 남은 수량 기록(히스토리용)
        int refundAmount = this.getRemainingSessions();

        this.status = UserSessionStatus.REFUNDED;
        this.registerEvent(SessionChangedEvent.refunded(this, modifier, -refundAmount));
    }


    // ----- 내부 통합 검증(상태 + Policy)
    private void validateForUse(OffsetDateTime now) {
        if (this.status != UserSessionStatus.ACTIVE) {
            throw new UserSessionsException(UserSessionErrorCode.INVALID_STATUS, this.status);
        }
        if (usedSessions >= this.totalSessions) {
            throw new UserSessionsException(UserSessionErrorCode.EXHAUSTED);
        }
        SessionPolicy.validateExpiry(this.expireAt, now );
    }


    private void validateForRestore() {
        SessionPolicy.validateRestoreAmount(this.usedSessions);

        // 환불 되었거나, 중지된 세션은 복구할 수 없음
        if (this.status == UserSessionStatus.REFUNDED) {
            throw new UserSessionsException(UserSessionErrorCode.INVALID_STATUS, this.status);
        }
    }

    private void validateRefund(OffsetDateTime now) {
        if (this.status == UserSessionStatus.REFUNDED) {
            throw new UserSessionsException(UserSessionErrorCode.INVALID_STATUS, this.status);
        }
        if(this.expireAt.isAfter(now)) {
            throw new UserSessionsException(UserSessionErrorCode.INVALID_STATUS, "기간이 지난 세션권은 환불 할 수 없습니다." );
        }
        SessionPolicy.validateForRefund(this.usedSessions, this.expireAt);

    }


    // -- 단순 헬퍼 (상태검증만)

    private static void validateNullUser(User user){
        if(user == null) throw new IllegalArgumentException("User의 값이 없습니다.");
    }

    private void validateExpired() {
        if(this.status == UserSessionStatus.DEACTIVATED ||
                this.status == UserSessionStatus.FULLY_USED ||
                this.status == UserSessionStatus.EXPIRED) {
            throw new UserSessionsException(UserSessionErrorCode.INVALID_STATUS, this.status);
        }
    }

    private void validateDeactivated() {
        if(this.status == UserSessionStatus.DEACTIVATED) {
            throw new UserSessionsException(UserSessionErrorCode.INVALID_STATUS, this.status);
        }
    }


    // 세션이 사용된적 없는가?
    public boolean isNotUsed(){
        return this.getRemainingSessions() == this.totalSessions;
    }

    // 환불 가능 여부 판단
    public void validateRefundable(){
        if(isNotUsed()) {
            throw new UserSessionsException(UserSessionErrorCode.ALREADY_USED);
        }
    }

    public int getRemainingSessions() {
        return this.totalSessions - this.usedSessions;
    }

    // --- 조회용
    public double calculateRefundRate(){
        return SessionPolicy.calculateRefundRate(this.usedSessions);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;

        UserSession that = (UserSession) o;

        return this.sessionId != null && this.sessionId.equals(that.sessionId);
    }

    @Override
    public int hashCode() {
        return (sessionId != null) ? sessionId.hashCode() : 0;
    }

}

/*
   이벤트 목적(UserSession History에 log를 쌓기위해)
    Modifier의 흐름

    1) 서비스 호출: session.use(modifier)
    2) 엔티티 확인: this.modifier = modifier; @Treansient에 임시보관
    3) 이벤트 생성: registerEvent(SessionChangedEvent.used(this.modifier)
    4) 리스너 처리: 리스너에서 UserssionHistory 엔티티 생성
    5) DB 저장: USER_SESSION_TB는 바뀐 세션횟수로 업데이트,
                USER_SESSION_HISTORY_TB는 누가 했는지 정보 저장.
 */