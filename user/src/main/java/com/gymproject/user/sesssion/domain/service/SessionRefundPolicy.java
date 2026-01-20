package com.gymproject.user.sesssion.domain.service;

import com.gymproject.common.policy.RefundDecision;
import com.gymproject.user.sesssion.domain.entity.UserSession;
import org.springframework.stereotype.Component;

@Component
public class SessionRefundPolicy {

    public RefundDecision calculate(UserSession userSession, long paidAmount) {
        // 비율 환산
        double refundRate = userSession.calculateRefundRate();

        // 남은 횟수가 전체 횟수와 동일해야만 전액 환불 가능
        if(refundRate >= 1.0){
            return new RefundDecision(true, paidAmount);
        }
        // 한 번이라도 사용했다면 환불 불가
        return new RefundDecision(false, 0L);
    }
}

/*
    [중요]
    SessionPolicy : 도메인 정책
    SessionRefundPolicy: 도메인 서비스 (환불규정을 해석)
    application serviceL 데이터베이스에서 세션을 찾아오고 작업의 순서를 관리
 */