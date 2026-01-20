package com.gymproject.user.membership.domain.entity;

import com.gymproject.common.security.Roles;
import com.gymproject.common.vo.Modifier;
import com.gymproject.user.common.domain.BaseEntity;
import com.gymproject.user.membership.domain.type.MembershipChangeType;
import com.gymproject.user.membership.domain.type.MembershipPlanType;
import com.gymproject.user.membership.domain.type.MembershipStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

import static java.time.Duration.between;

@Entity
@Table(name = "USER_MEMBERSHIP_HISTORY_TB")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMembershipHistory extends BaseEntity {

    @Id
    @Column(name = "membership_history_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private MembershipChangeType changeType;

    @Column(name = "started_at_snapshot", nullable = false)
    private OffsetDateTime startedAtSnapshot;

    @Column(name = "before_expired_at_snapshot")
    private OffsetDateTime beforeExpiredAtSnapshot;

    @Column(name = "after_expired_at_snapshot", nullable = false)
    private OffsetDateTime afterExpiredAtSnapshot;

    @Column(name = "amount_days")
    private int amountDays; // 변동일 수

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private MembershipPlanType membershipPlanType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MembershipStatus status;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "modifier_id", nullable = false)
    private Long modifierId;

    @Column(name = "modifier_role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Roles modifierRole;

    @Column(name = "modifier_name")
    private String modifierName;

    @Builder
    public UserMembershipHistory(Long membershipId, Long userId, MembershipChangeType changeType,
                                 OffsetDateTime startedAtSnapshot, String detail, int amountDays,
                                 OffsetDateTime beforeExpiredAtSnapshot, OffsetDateTime afterExpiredAtSnapshot,
                                 MembershipPlanType planTypeSnapshot, MembershipStatus status,
                                 String description, Modifier modifier) {
        this.membershipId = membershipId;
        this.userId = userId;
        this.changeType = changeType;
        this.startedAtSnapshot = startedAtSnapshot;
        this.beforeExpiredAtSnapshot = beforeExpiredAtSnapshot;
        this.afterExpiredAtSnapshot = afterExpiredAtSnapshot;
        this.amountDays = amountDays;
        this.description = detail;
        this.status = status;
        this.membershipPlanType = planTypeSnapshot;
        this.description = description;
        this.modifierId = modifier.id();
        this.modifierRole = modifier.role();
        this.modifierName = modifier.name();
    }


    // 신규 구매
    public static UserMembershipHistory recordPurchase(UserMembership userMembership, Modifier modifier) {

        long totalDays = between(userMembership.getStartedAt(), userMembership.getExpiredAt()).toDays();

        return UserMembershipHistory.builder()
                .membershipId(userMembership.getMembershipId())
                .userId(userMembership.getUser().getUserId())
                .changeType(MembershipChangeType.PURCHASE)
                .startedAtSnapshot(userMembership.getStartedAt())
                .beforeExpiredAtSnapshot(null)
                .afterExpiredAtSnapshot(userMembership.getExpiredAt())
                .amountDays((int) totalDays)
                .description("신규 구매")
                .status(userMembership.getStatus())
                .planTypeSnapshot(userMembership.getPlanType())
                .modifier(modifier)
                .build();
    }


    // 기간 연장
    public static UserMembershipHistory recordExtend(UserMembership userMembership,
                                                     OffsetDateTime beforeExpiredDate,
                                                     Modifier modifier) {

        // [중요] 연장된 일수 계산 (새 만료일 - 이전 만료일)
        long days = between(beforeExpiredDate, userMembership.getExpiredAt()).toDays();

        return UserMembershipHistory.builder()
                .membershipId(userMembership.getMembershipId())
                .userId(userMembership.getUser().getUserId())
                .changeType(MembershipChangeType.EXTEND)
                .startedAtSnapshot(userMembership.getStartedAt())
                .beforeExpiredAtSnapshot(beforeExpiredDate)
                .afterExpiredAtSnapshot(userMembership.getExpiredAt())
                .amountDays((int) days)
                .description("멤버십 기간 연장 (" + (int) days + "일 추가")
                .planTypeSnapshot(userMembership.getPlanType())
                .status(userMembership.getStatus())
                .modifier(modifier)
                .build();
    }

    // 환불
    public static UserMembershipHistory recordRollback(UserMembership membership,
                                                       OffsetDateTime beforeExpiredDate,
                                                       Modifier modifier) {
        // 줄어든 일수 계산 (새 만료일 - 이전 만료일 = 음수 결과 나옴)
        long reducedDays = between(beforeExpiredDate, membership.getExpiredAt()).toDays();

        return UserMembershipHistory.builder()
                .membershipId(membership.getMembershipId())
                .userId(membership.getUser().getUserId())
                .changeType(MembershipChangeType.ROLLBACK)
                .startedAtSnapshot(membership.getStartedAt())
                .beforeExpiredAtSnapshot(beforeExpiredDate)
                .afterExpiredAtSnapshot(membership.getExpiredAt())
                .amountDays((int) reducedDays)
                .status(membership.getStatus())
                .planTypeSnapshot(membership.getPlanType())
                .description("결제 취소에 따른 기간 롤백")
                .modifier(modifier)
                .build();
    }

    // 기간 만료
    public static UserMembershipHistory recordExpire(UserMembership membership, Modifier modifier) {
        return UserMembershipHistory.builder()
                .membershipId(membership.getMembershipId())
                .userId(membership.getUser().getUserId())
                .changeType(MembershipChangeType.EXPIRED)
                .startedAtSnapshot(membership.getStartedAt())
                .beforeExpiredAtSnapshot(membership.getExpiredAt())
                .afterExpiredAtSnapshot(membership.getExpiredAt()) // 날짜 변화는 없음
                .amountDays(0)
                .status(MembershipStatus.EXPIRED)
                .planTypeSnapshot(membership.getPlanType())
                .description("기간 만료로 인한 자동 종료")
                .modifier(modifier)
                .build();
    }

    // 일시 정지
    public static UserMembershipHistory recordSuspend(UserMembership membership, Modifier modifier, String reason) {
        return UserMembershipHistory.builder()
                .membershipId(membership.getMembershipId())
                .userId(membership.getUser().getUserId())
                .changeType(MembershipChangeType.SUSPEND)
                .startedAtSnapshot(membership.getStartedAt())
                .beforeExpiredAtSnapshot(membership.getExpiredAt())
                .afterExpiredAtSnapshot(membership.getExpiredAt()) // 날짜 변화는 없음
                .amountDays(0)
                .status(MembershipStatus.SUSPENDED) // 상태가 정지로 변경됨을 기록
                .planTypeSnapshot(membership.getPlanType())
                .description("멤버십 일시 정지: " + reason)
                .modifier(modifier)
                .build();
    }

    // 재시작
    public static UserMembershipHistory recordResume(UserMembership membership,
                                                     OffsetDateTime beforeExpiredDate,
                                                     int amountDays,
                                                     Modifier modifier) {
        return UserMembershipHistory.builder()
                .membershipId(membership.getMembershipId())
                .userId(membership.getUser().getUserId())
                .changeType(MembershipChangeType.RESUME)
                .startedAtSnapshot(membership.getStartedAt())
                .beforeExpiredAtSnapshot(beforeExpiredDate)
                .afterExpiredAtSnapshot(membership.getExpiredAt())
                .amountDays(amountDays)
                .status(MembershipStatus.ACTIVE) // 다시 활성화
                .planTypeSnapshot(membership.getPlanType())
                .description("일시 정지 해제 (정지 기간만큼 " + amountDays + "일 연장)")
                .modifier(modifier)
                .build();
    }

    // 전액 환불
    public static UserMembershipHistory recordCancel(UserMembership membership, OffsetDateTime beforeExpiredDate, Modifier modifier) {

        // 취소 시점부터 원래 남았던 기간을 계산
        long remainingDays = between(beforeExpiredDate, membership.getExpiredAt()).toDays();

        return UserMembershipHistory.builder()
                .membershipId(membership.getMembershipId())
                .userId(membership.getUser().getUserId())
                .changeType(MembershipChangeType.CANCELLED)
                .startedAtSnapshot(membership.getStartedAt())
                .beforeExpiredAtSnapshot(membership.getExpiredAt())
                .afterExpiredAtSnapshot(beforeExpiredDate)
                .amountDays((int) -remainingDays)
                .status(MembershipStatus.CANCELLED)
                .planTypeSnapshot(membership.getPlanType())
                .description("미사용 전액 환불/취소 처리")
                .modifier(modifier)
                .build();
    }

}
