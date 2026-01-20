package com.gymproject.user.infrastructure.adapter;

import com.gymproject.common.policy.RefundContext;
import com.gymproject.common.policy.RefundDecision;
import com.gymproject.user.membership.domain.entity.UserMembership;
import com.gymproject.user.membership.domain.policy.MembershipRefundPolicy;
import com.gymproject.user.membership.exception.UserMembershipErrorCode;
import com.gymproject.user.membership.exception.UserMembershipException;
import com.gymproject.user.membership.infrastructure.persistence.UserMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class MembershipRefundStrategy implements RefundStrategy {

    private final UserMembershipRepository userMembershipRepository;
    private final MembershipRefundPolicy membershipRefundPolicy;
//    private final JsonSerializer jsonSerializer;

    @Override
    public boolean supports(String productCategory) {
        return productCategory.equalsIgnoreCase("MEMBERSHIP");
    }

    @Override
    public RefundDecision calculate(RefundContext context) {
        UserMembership membership = userMembershipRepository.findById(context.sourceId())
                .orElseThrow(() -> new UserMembershipException(UserMembershipErrorCode.NOT_FOUND));

        return membershipRefundPolicy.calculate(
                membership,
                OffsetDateTime.now(),
                context.paidAmount()
        );
    }
}
