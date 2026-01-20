package com.gymproject.payment.payment.application.service.processor;

import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.policy.RefundContext;
import com.gymproject.common.policy.RefundDecision;
import com.gymproject.common.port.payment.RefundPolicyPort;
import com.gymproject.common.security.Roles;
import com.gymproject.payment.application.port.PaymentGatewayPort;
import com.gymproject.payment.payment.domain.entity.Payment;
import com.gymproject.payment.product.domain.type.ProductCategory;
import com.gymproject.payment.payment.exception.PaymentErrorCode;
import com.gymproject.payment.payment.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionRefundProcessor implements RefundProcessor {

    private final RefundPolicyPort refundPolicyPort; // Session 도메인에서 환불 계산해주는 포트
    private final PaymentGatewayPort paymentGatewayPort;

    @Override
    public boolean supports(ProductCategory category) {
        return category == ProductCategory.SESSION;
    }

    @Override
    public RefundDecision process(Payment payment, UserAuthInfo userAuthInfo) {
        // 1/ 환불 금액 결정
        RefundDecision decision = resolveRefundDecision(payment);

        // 2. 환불 불가하면 예외 발생
        if (!decision.refundable()) {
            throw new PaymentException(PaymentErrorCode.REFUND_REJECTED);
        }

        // 3. PG사에 결제 취소 요청(외부 통신)
        paymentGatewayPort.refund(payment.getProviderPaymentId(),decision.refundAmount());

        // 4. 내부 DB 상태 업데이트
        updatePaymentStatus(payment, userAuthInfo);

        // 5. 응답용 환불금액 전달
        return decision;
    }

    /*
        상황에 따라 환불 정책을 결정
        - 세션권 생성 실패 -> 전액 환불
        - 일반 요청 -> 정책 포트에게 계산 위임
     */
    private RefundDecision resolveRefundDecision(Payment payment) {
        if(payment.getSourceId() == null){
            // 시스템 에러는 전액환불
            return new RefundDecision(true, payment.getAmountCents());
        }
        return new RefundDecision(false, payment.getAmountCents());
    }

    /*
        환불 주체(Actor)를 판단하여 상태 업데이트
     */
    private void updatePaymentStatus(Payment payment, UserAuthInfo userAuthInfo) {
        //SourceId가 없으면 시스템 오류로 인한 강제 환불
        if(payment.getSourceId() == null){
            payment.refundRequested(-1L, Roles.SYSTEM);
            return;
        }

        // 정상 환불 요청한 사람은 사용자의 정보로 기록
        payment.refundRequested(userAuthInfo.getUserId(), userAuthInfo.getRole());
    }


    // Context 생성 헬퍼
    private RefundContext createRefundContext(Payment payment) {
        return new RefundContext(
                payment.getSnapshotProductCategory().name(),
                payment.getSnapshotProductName(),
                payment.getContractSnapshot(),
                payment.getCreatedAt(),
                payment.getAmountCents(),
                payment.getSourceId()
        );
    }

}
