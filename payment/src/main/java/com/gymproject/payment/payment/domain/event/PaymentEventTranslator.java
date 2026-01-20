package com.gymproject.payment.payment.domain.event;

import com.gymproject.common.event.integration.PaymentSucceededEvent;
import com.gymproject.payment.payment.domain.entity.Payment;
import com.gymproject.payment.payment.domain.type.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentEventTranslator {

    private final ApplicationEventPublisher applicationEventPublisher;

    /* 결제 정보가 DB에 완전히 저장된 후 실행해라라는 뜻
        따라서 payment_tb에는 update 쿼리가 날아가고 트랜잭션이 끝남.

        근데 MembershipPaymentListener가 실행됨
        이미 DB 연결 통로가 닫히기 직전이나 완료된 상태이기 때문에
        UserMembershipService을 호출해도 트랜잭션이 종료된것으로 간주하여 새로운 변경사항을 Db에 반영하지얺움

        따라서 새로운 트랜잭션을 열어줘야함.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void translate(PaymentChangedEvent internalEvent){

        // [중요] 결제 성공이 아닌 이벤트들은 무시
        if(internalEvent.getNewStatus() != PaymentStatus.CAPTURED){
            return;
        }

        // 1. Payment 엔티티 가져오기
        Payment payment = internalEvent.getPayment();

        // 외부 이벤트에 필요한 내용꺼내기
        Long userId = payment.getUserId();
        Long paymentId = payment.getPaymentId();
        Long amount = payment.getAmountCents();
        String category = payment.getSnapshotProductCategory().name();
        String code = payment.getSnapshotPlanName();
        String contract = payment.getContractSnapshot(); // 멤버십 시작일 등 계약 내용

        // 2. 외부 이벤트 만들기
        PaymentSucceededEvent externalEvent =
                new PaymentSucceededEvent(userId, paymentId, amount, category, code, contract, payment.getProviderPaymentId());

        // 3. 이벤트 발행
        applicationEventPublisher.publishEvent(externalEvent);

    }

}
