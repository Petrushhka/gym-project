package com.gymproject.user.membership.infrastructure.listener;

import com.gymproject.common.event.integration.PaymentSucceededEvent;
import com.gymproject.common.port.payment.PaymentPort;
import com.gymproject.user.membership.application.UserMembershipService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MembershipPaymentListener {

    Logger log = LoggerFactory.getLogger(MembershipPaymentListener.class);

    private final UserMembershipService userMembershipService;
    private final PaymentPort paymentPort;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW) // [중요]
    public void handle(PaymentSucceededEvent event) {

        // 1. 카테고리가 안맞을 경우 무시
        if (!event.category().equalsIgnoreCase("MEMBERSHIP")) {
            return;
        }

        // 2. 서비스에 비지니스 처리 위임
        try {
            log.info("Received PaymentSucceededEvent");
            userMembershipService.purchaseMembership(
                    event.userId(),
                    event.planeCode(),
                    event.paymentId(),
                    event.contractJson()
            );
            log.info("멤버십 상품 발급 성공: PaymentId={}, UserId = {}", event.paymentId(), event.userId());
        } catch (Exception e) {

            log.error("멤버십 상품 발급 실패: PaymentId={}, UserId = {}, 에러 = {}", event.paymentId(), event.userId(), e.getMessage());

            // 멤버십 상품 발급 실패시에 환불 처리 해야함
            paymentPort.compensate(event.paymentKey());

        }
    }
}
