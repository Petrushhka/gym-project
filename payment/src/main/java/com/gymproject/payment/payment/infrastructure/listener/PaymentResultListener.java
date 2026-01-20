package com.gymproject.payment.payment.infrastructure.listener;

import com.gymproject.common.event.integration.ProductCreatedEvent;
import com.gymproject.payment.payment.domain.entity.Payment;
import com.gymproject.payment.payment.exception.PaymentErrorCode;
import com.gymproject.payment.payment.exception.PaymentException;
import com.gymproject.payment.payment.infrastructure.persistence.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentResultListener {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductCreatedEvent event) {

        // 1. payment 객체 찾아오기
        Payment payment = paymentRepository.findById(event.paymentId())
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // 2. sourceId 연결하기
        /// [중요] 결제 시점에는 상품권 sourceId가 없기 때문에 save된 시점 이후에 다시 바인딩하는 작업임
        payment.bindSourceId(event.sourceId());

        paymentRepository.save(payment);
    }

}
