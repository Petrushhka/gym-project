package com.gymproject.user.sesssion.infrastructure.listener;

import com.gymproject.common.event.integration.PaymentSucceededEvent;
import com.gymproject.common.port.payment.PaymentPort;
import com.gymproject.user.sesssion.application.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SessionPaymentListener {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final UserSessionService userSessionService;
    private final PaymentPort paymentPort;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(PaymentSucceededEvent event) {

        // 1. 카테고리가 Session이 아니면 무시
        if (!event.category().equalsIgnoreCase("SESSION")) {
            return;
        }
        // 2. 서비스에게 모든 비지니스 처리를 위힘
        try {
            userSessionService.purchaseSession(event.userId(), event.planeCode(), event.paymentId(), event.contractJson());
            log.info("세션 상품 발급 성공: PaymentId={}, UserId={}", event.paymentId(), event.userId());
        }
        catch (Exception e) {
            log.error("존재하지 않는 PT 상품: {}", event.planeCode());
            paymentPort.compensate(event.paymentKey());
        }
    }
}

/*
    [중요] 리스너의 책임은 어디까지?
    리스너도 이벤트 전용 "어댑터"임. 따라서 어댑터와 같이 번역기 역할까지만 해주면됨.

    그리고 나머지는 응용 서비스에서 처리!

    [현재 프로젝트 내부의 요청 종류]
    Controller : 외부 사용자의 요청(HTTP)
    Adapter: 다른 모듈의 요청(Port)
    Listener: 시스템 내부의 요청(Event)

    새로운 비지니스 흐름을 시작해야한다면 -> Service단에서
    이미 끝난 비지니스의 부가작업(로그)을 한다면 -> Adapter에서 수행


 */