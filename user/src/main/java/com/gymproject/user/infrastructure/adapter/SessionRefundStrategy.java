package com.gymproject.user.infrastructure.adapter;

import com.gymproject.common.policy.RefundContext;
import com.gymproject.common.policy.RefundDecision;
import com.gymproject.user.sesssion.domain.entity.UserSession;
import com.gymproject.user.sesssion.domain.service.SessionRefundPolicy;
import com.gymproject.user.sesssion.exception.UserSessionErrorCode;
import com.gymproject.user.sesssion.exception.UserSessionsException;
import com.gymproject.user.sesssion.infrastructure.persistence.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionRefundStrategy implements RefundStrategy {
    private final UserSessionRepository userSessionRepository;
    private final SessionRefundPolicy sessionRefundPolicy;
//    private final JsonSerializer jsonSerializer;

    @Override
    public boolean supports(String productCategory) {
        return productCategory.equalsIgnoreCase("SESSION");
    }

    @Override
    public RefundDecision calculate(RefundContext context) {
        UserSession userSession = userSessionRepository.findById(context.sourceId())
                .orElseThrow(()-> new UserSessionsException(UserSessionErrorCode.NOT_FOUND));

        return sessionRefundPolicy.calculate(userSession, context.paidAmount());
    }
}
