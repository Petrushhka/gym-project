package com.gymproject.user.membership.domain.type;

import com.gymproject.user.membership.exception.UserMembershipErrorCode;
import com.gymproject.user.membership.exception.UserMembershipException;

import java.time.OffsetDateTime;
import java.util.Arrays;

public enum MembershipPlanType {
    MONTH_1("MEMBERSHIP_1M", 1), // 1개월
    MONTH_3("MEMBERSHIP_3M", 3), // 3개월
    MONTH_6("MEMBERSHIP_6M", 6), // 6개월
    MONTH_12("MEMBERSHIP_12M", 12); // 12개월

    private final String code;
    private final int months;

    MembershipPlanType(String code, int months) {
        this.code = code;
        this.months = months;
    }

    // 멤버십 유효기간
    public OffsetDateTime calculateExpiredAt(OffsetDateTime startAt) {
        return startAt.plusMonths(months);
    }

    public static MembershipPlanType findByCode(String planCode) {
       return Arrays.stream(MembershipPlanType.values())
                .filter(type -> type.code.equals(planCode)) // code끼리 비교
                .findFirst()
                .orElseThrow(() -> new UserMembershipException(UserMembershipErrorCode.INVALID_PLAN_TYPE, String.format("코드 번호 확인 필요: %s", planCode))); // "지원하지 않는 상품 코드입니다" 에러
    }
}
