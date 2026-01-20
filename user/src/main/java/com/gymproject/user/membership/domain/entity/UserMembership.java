package com.gymproject.user.membership.domain.entity;

import com.gymproject.common.vo.Modifier;
import com.gymproject.user.membership.domain.policy.MembershipPolicy;
import com.gymproject.user.profile.domain.entity.User;
import com.gymproject.user.membership.domain.event.MembershipChangedEvent;
import com.gymproject.user.membership.domain.type.MembershipPlanType;
import com.gymproject.user.membership.domain.type.MembershipStatus;
import com.gymproject.user.membership.exception.UserMembershipErrorCode;
import com.gymproject.user.membership.exception.UserMembershipException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.Duration;
import java.time.OffsetDateTime;

@Getter
@Entity
@ToString(exclude = "user")
@Table(name = "USER_MEMBERSHIP_TB")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMembership extends AbstractAggregateRoot<UserMembership> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private MembershipPlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MembershipStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "expired_at", nullable = false)
    private OffsetDateTime expiredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "suspend_start_at")
    private OffsetDateTime suspendStartAt; // 정지 시작일

    @Column(name = "suspend_end_at")
    private OffsetDateTime suspendEndAt; // 정지 종료일

    // [추가] 동시성 제어를 위한 낙관적 락
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    private UserMembership(User user,
                           MembershipPlanType planType,
                           OffsetDateTime startedAt,
                           OffsetDateTime expiredAt) {
        this.user = user;
        this.planType = planType;
        this.startedAt = startedAt;
        this.expiredAt = expiredAt;
        this.status = MembershipStatus.ACTIVE;
    }

    // 1. 생성
    public static UserMembership create(User user,
                                        MembershipPlanType planType,
                                        OffsetDateTime startDate,
                                        OffsetDateTime now,
                                        Modifier modifier) {

        validateCreation(user, planType, startDate, now);

        MembershipPolicy.validateStartDate(startDate, now);

        UserMembership membership = new UserMembership(
                user, planType, startDate,
                planType.calculateExpiredAt(startDate));

        membership.registerEvent(
                MembershipChangedEvent.purchased(
                        membership, modifier));
        return membership;
    }


    /*
      12월 16일에 만료되었다면?
      스케쥴러나 배치가 돌지 않는다면, 여전히 ACTIVCE인 상태
      따라서 새로 결제하면,
      findByUserAndStatus에서 ACTIVE인 Membership이 튀어나와버리게됨.
      따라서 기간이 만료된 날짜를 기준으로 연장시켜줘야함.

   */
    // 2. 연장
    public void extend(MembershipPlanType newPlan, Modifier modifier, OffsetDateTime now) {
        // 연장 가능한 상태인지
        validateForExtend(now);

        // 스냅샷용(기존 만료일)
        OffsetDateTime beforeExpiredDate = this.expiredAt;

        // 기존 멤버십이 종료되지 않았으면 기존 만료일부터 연장, 아니면 지금부터 연장
        OffsetDateTime baseDate = this.calculateExtensionStartDate(now);

        // 기존 만료일을 기준으로 기간을 더해서 업데이트
        this.expiredAt = newPlan.calculateExpiredAt(baseDate);
        this.planType = newPlan;

        // 만료상태였을 경우 다시 활성화
        if (this.status == MembershipStatus.EXPIRED) {
            this.status = MembershipStatus.ACTIVE;
        }

        this.registerEvent(MembershipChangedEvent.extended(
                this, beforeExpiredDate, modifier));
    }

    // 3. 일시 정지
    public void suspend(Modifier modifier, String reason,
                        OffsetDateTime startAt, OffsetDateTime endAt,
                        OffsetDateTime now) {
        validateForSuspend(startAt, endAt, now);

        long requestDays = Duration.between(startAt, endAt).toDays();

        this.status = MembershipStatus.SUSPENDED;
        this.suspendStartAt = startAt;
        this.suspendEndAt = this.suspendStartAt.plusDays(requestDays);

        this.registerEvent(MembershipChangedEvent.suspended(this, reason, modifier));
    }

    // 4.  정지 해제 - 하루 단위로만 정지(23시간 정지는 정지된게 아니라고 간주)
    public void resume(Modifier modifier, OffsetDateTime suspendEndAt) {
        validateForResume();

        // 스냅샷용(기존 만료일)
        OffsetDateTime beforeExpiredDate = this.expiredAt;

        // 실제 정지 기간 계산
        long suspendedDays = Duration.between(this.suspendStartAt, suspendEndAt).toDays();
        if (suspendedDays < 0) {
            suspendedDays = 0;
        }

        // 기간 연장 및 상태 복구
        this.expiredAt = this.expiredAt.plusDays(suspendedDays);
        this.status = MembershipStatus.ACTIVE;
        this.suspendStartAt = null;
        this.suspendEndAt = null;

        this.registerEvent(MembershipChangedEvent.resumed(this, beforeExpiredDate, (int) suspendedDays, modifier));
    }


    // 5. 기간 만료(테스트용)
    public void expire(Modifier modifier, OffsetDateTime now) {
        if (!isExpirable(now)) return;

        this.status = MembershipStatus.EXPIRED;
        this.registerEvent(MembershipChangedEvent.expire(this, modifier));
    }

    // 6. 전액 환불(아예 사용하지 않았을 때만)
    public void cancel(Modifier modifier, OffsetDateTime refundDate) {
        validateForCancel(refundDate);

        // 스냅샷용(기존만료일)
        OffsetDateTime beforeExpiredDate = this.expiredAt;

        // 2. 상태 변경
        this.status = MembershipStatus.CANCELLED;
        this.expiredAt = refundDate;

        // 3.  이벤트 발행
        this.registerEvent(MembershipChangedEvent.cancel(this, modifier, beforeExpiredDate));

    }

    // 7.  부분 환불( 사용 후 14일 이내에만)
    public void rollbackByRefund(Modifier modifier, OffsetDateTime refundDate) {
        validateForRollback(refundDate);

        // 스냅샷용
        OffsetDateTime beforeExpiredDate = this.expiredAt;

        this.expiredAt = refundDate;
        this.status = MembershipStatus.CANCELLED;

        this.registerEvent(MembershipChangedEvent.rollback(this, beforeExpiredDate, 0, modifier));
    }

    //* --------------------------- 검증 로직 (상태 + Policy 메서드 결합)-------------------------------

    private static void validateCreation(User user, MembershipPlanType planType, OffsetDateTime startDate, OffsetDateTime now) {
        if(user == null) {
            throw new IllegalArgumentException("사용자 정보는 필수입니다.");
        }
        if(planType == null) {
            throw new IllegalArgumentException("멤버십 플랜 정보는 필수입니다.");
        }
        if(startDate.isBefore(now)){
            throw new UserMembershipException(UserMembershipErrorCode.INVALID_DATE);
        }
    }

    public void validateForExtend(OffsetDateTime now) {
        if(this.status == MembershipStatus.CANCELLED || this.status == MembershipStatus.SUSPENDED) {
            throw new UserMembershipException(UserMembershipErrorCode.INVALID_STATUS, this.status);
        }
        if(this.status == MembershipStatus.EXPIRED){
        MembershipPolicy.validateExtendablePeriod(this.expiredAt, now);
        }
    }

    private void validateForSuspend(OffsetDateTime startAt, OffsetDateTime endAt, OffsetDateTime now) {
        if (this.status != MembershipStatus.ACTIVE) {
            throw new UserMembershipException(UserMembershipErrorCode.NOT_ACTIVE);
        }

        MembershipPolicy.validateSuspendStartTime(startAt, now);
        MembershipPolicy.validateSuspendPeriod(startAt, endAt);
    }

    private void validateForResume() {
        if (this.status != MembershipStatus.SUSPENDED) {
            throw new UserMembershipException(UserMembershipErrorCode.NOT_SUSPENDED, this.status);
        }
    }

    private boolean isExpirable(OffsetDateTime now) {
        if (this.status != MembershipStatus.ACTIVE) {return false;}
        if (now.isBefore(this.expiredAt)) {
            throw new UserMembershipException(UserMembershipErrorCode.NOT_YET_EXPIRED, this.expiredAt);}
        return true;
    }

    private void validateForCancel(OffsetDateTime now) {
        if (this.status == MembershipStatus.CANCELLED || this.status == MembershipStatus.EXPIRED) {
            throw new UserMembershipException(UserMembershipErrorCode.ALREADY_PROCESSED, this.status);
        }
        MembershipPolicy.validateCancelPeriod(this.startedAt, now);
    }

    private void validateForRollback(OffsetDateTime refundDate) {
        if(this.status == MembershipStatus.CANCELLED || this.status == MembershipStatus.EXPIRED) {
            throw new UserMembershipException(UserMembershipErrorCode.ALREADY_PROCESSED, this.status);
        }
            MembershipPolicy.validateRollbackPeriod(this.startedAt, refundDate);
        }


    // ------ 조회용 -----

    public void validateActiveUtil(OffsetDateTime requiredDate) {
        // 1. 상태 검증
        if(this.status != MembershipStatus.ACTIVE) {
            throw new UserMembershipException(UserMembershipErrorCode.NOT_ACTIVE);
        }

        // 2. 기간 검증
        if(this.expiredAt.isBefore(requiredDate)) {
            throw new UserMembershipException(UserMembershipErrorCode.INSUFFICIENT_MEMBERSHIP_PERIOD);
        }
    }

    //현재 시점을 기준으로 환불 가능한 비율을 계산
    public double calculateRefundRate(OffsetDateTime now){
       return  MembershipPolicy.calculateRefundRate(this.startedAt, now);
    }

    public OffsetDateTime calculateExtensionStartDate(OffsetDateTime now) {
        // 1. 만료기간이 현재보다 과거면 만료기간을 기준으로 연장
        // 2. 만료기간이 지났으면 지금을 기준으로 연장
        return MembershipPolicy.calculateExtensionBaseDate(this.expiredAt, now);
    }

}
