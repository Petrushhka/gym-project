package com.gymproject.payment.payment.infrastructure.adapter;

import com.gymproject.common.port.payment.PaymentPort;
import com.gymproject.payment.payment.application.dto.InitiatePaymentCommand;
import com.gymproject.payment.payment.application.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class PaymentAdapter implements PaymentPort {

    private final PaymentService paymentService;

    @Override
    public String readyToPayMembership(Long userId, String productName, String productCode, Long amount, OffsetDateTime startDate, OffsetDateTime endDate, String type) {
        // 1. Membership에서 받은 데이터를 Payment 모듈의 Command로 변환
        // ProductCode나 Category 같은 정보가 더 필요하다면 Service에서 넘겨바도록 Port 수정
        InitiatePaymentCommand command = InitiatePaymentCommand.forMembership(
                userId,
                productName,
                productCode,
                amount,
                startDate,
                endDate,
                type
        );

        return paymentService.initiatePayment(command);
    }

    @Override
    public String readyToPaySession(Long userId, String productName, String productCode, Long amount,
                                    Integer totalSessionCount, OffsetDateTime startDate, OffsetDateTime endDate) {
           // 1. Session에서 받은 데이터를 Payment에 필요한 Command로 변환
        InitiatePaymentCommand command = InitiatePaymentCommand.forSession(
                userId,
                productName,
                productCode,
                amount,
                totalSessionCount,
                startDate,
                endDate
        );

        return paymentService.initiatePayment(command);
    }

    @Override
    public void compensate(String paymentKey) {
        paymentService.compensatePayment(paymentKey);
    }
}
