package com.gymproject.user.membership.domain.policy;

import com.gymproject.common.policy.RefundDecision;
import com.gymproject.user.membership.domain.entity.UserMembership;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class MembershipRefundPolicy {
    /**
     * 멤버십 환불 정책에 따른 금액 계산
     */
    public RefundDecision calculate(UserMembership membership, OffsetDateTime now, long paidAmount) {

        double refundRate = membership.calculateRefundRate(now);

        // 1. 시작 전이라면 전액 환불
        if (refundRate >= 1.0) {
            return new RefundDecision(true, paidAmount);
        }

        // 2. 시작 후 14일 이내라면 일할 계산(Pro-rata)
        if (refundRate > 0) {
            long refundableAmount = (long) (paidAmount * refundRate);
            return new RefundDecision(true, refundableAmount);
        }

        // 3. 14일 이후라면 환불 불가
        return new RefundDecision(false, 0L);
    }


}
