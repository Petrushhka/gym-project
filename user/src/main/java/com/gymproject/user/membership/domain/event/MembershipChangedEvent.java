package com.gymproject.user.membership.domain.event;

import com.gymproject.common.vo.Modifier;
import com.gymproject.user.membership.domain.entity.UserMembership;
import com.gymproject.user.membership.domain.type.MembershipChangeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@RequiredArgsConstructor
public class MembershipChangedEvent {

    private final UserMembership userMembership;
    private final Modifier modifier;
    private final MembershipChangeType type;
    private final OffsetDateTime beforeExpiredDate; // 연장/환불 전 멤버십만료 날짜
    private final int amountDays; // 변동 일수
    private final String description; // 사유

    public static class Builder{
        private final UserMembership userMembership;
        private final Modifier modifier;
        private MembershipChangeType type;
        private OffsetDateTime beforeDate;
        private int amountDays;
        private String description;

        public Builder(UserMembership userMembership, Modifier modifier) {
            this.userMembership = userMembership;
            this.modifier = modifier;
        }

        public Builder action(MembershipChangeType type) {
            this.type = type;
            return this;
        }

        public Builder snapshot(OffsetDateTime beforeDate, int amountDays) {
            this.beforeDate = beforeDate;
            this.amountDays = amountDays;
            return this;
        }

        public Builder detail(String description) {
            this.description = description;
            return this;
        }
        public MembershipChangedEvent build() {
            return new MembershipChangedEvent(this);
        }
    }

    private MembershipChangedEvent(Builder builder) {
        this.userMembership = builder.userMembership;
        this.modifier = builder.modifier;
        this.type = builder.type;
        this.beforeExpiredDate = builder.beforeDate;
        this.amountDays = builder.amountDays;
        this.description = builder.description;
    }

    // *----------------------------정적 팩토리 메서드 ------------------

    // 1. 구매
    public static MembershipChangedEvent purchased(UserMembership membership, Modifier modifier) {
        return new Builder(membership, modifier)
                .action(MembershipChangeType.PURCHASE)
                .build();
    }

    // 2. 연장
    public static MembershipChangedEvent extended(UserMembership membership, OffsetDateTime beforeExpiredDate, Modifier modifier) {
        return new Builder(membership, modifier)
                .action(MembershipChangeType.EXTEND)
                .snapshot(beforeExpiredDate, 0) // 일수 계산은 리스너에서
                .build();
    }

    // 3. 일시정지
    public static MembershipChangedEvent suspended(UserMembership membership, String reason, Modifier modifier) {
        return new Builder(membership, modifier)
                .action(MembershipChangeType.SUSPEND)
                .detail(reason)
                .build();
    }

    // 4. 정지 해체
    public static MembershipChangedEvent resumed(UserMembership membership, OffsetDateTime beforeExpiredDate, int amountDays, Modifier modifier) {
        return new Builder(membership, modifier)
                .action(MembershipChangeType.RESUME)
                .snapshot(beforeExpiredDate, amountDays)
                .build();
    }

    // 5. 만료
    public static MembershipChangedEvent expire(UserMembership membership, Modifier modifier) {
        return new Builder(membership, modifier)
                .action(MembershipChangeType.EXPIRED)
                .build();
    }

    // 6. 취소
    public static MembershipChangedEvent cancel(UserMembership membership, Modifier modifier, OffsetDateTime beforeExpiredDate) {
        return new Builder(membership, modifier)
                .action(MembershipChangeType.CANCELLED)
                .snapshot(beforeExpiredDate, 0)
                .build();
    }

    // 7. 롤백
    public static MembershipChangedEvent rollback(UserMembership membership, OffsetDateTime beforeExpiredDate, int amountDays, Modifier modifier) {
        return new Builder(membership, modifier)
                .action(MembershipChangeType.ROLLBACK)
                .snapshot(beforeExpiredDate, 0)
                .build();
    }




}
