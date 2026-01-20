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
public class MembershipRefundProcessor implements RefundProcessor {

    private final RefundPolicyPort refundPolicyPort; // 멤서십 모듈에서 환불 정책을 계산해주는 포트
    private final PaymentGatewayPort paymentGatewayPort;

    @Override
    public boolean supports(ProductCategory category) {
        return category == ProductCategory.MEMBERSHIP;
    }

    @Override
    public RefundDecision process(Payment payment, UserAuthInfo userAuthInfo) {

        // 1. 환불 금액과 정책을 확정(에러 보상 or 일반 환불)
        RefundDecision decision = resolveRefundDecision(payment);

        // 2. 환불 불가능하면 예외 발생
        if(!decision.refundable()){
            throw new PaymentException(PaymentErrorCode.REFUND_REJECTED);
        }

        // 3. PG사에 결제 취소 요청(외부 통신)
        paymentGatewayPort.refund(payment.getProviderPaymentId(), decision.refundAmount());

        // 4. 내부 DB 상태 업데이트(누가 환불했는지)
        updatePaymentStatus(payment, userAuthInfo);

        // 5. 응답용으로 환불액 반환
        return decision;
    }

    /*
        상황에 따라 환불 정책을 결정
        - 멤버십 생성 실패 등에 인한 에러 -> 전액환불
        - 일반 요청 -> 정책 포트에 계산 위임
     */
    private RefundDecision resolveRefundDecision(Payment payment) {
        if (payment.getSourceId() == null) {
            // RefundDecision 생성자가 (금액, 환불가능여부)를 받음
            return new RefundDecision(true, payment.getAmountCents());
        }

        return refundPolicyPort.decide(createRefundContext(payment));
    }

    /*
        환불 주체(Actor)를 판단하여 상태를 업데이트
     */
    private void updatePaymentStatus(Payment payment, UserAuthInfo userAuthInfo) {
        // SourceId가 없으면 시스템 오류로 인한 강제 환불로 간주
        if (payment.getSourceId() == null) {
            payment.refundRequested(-1L, Roles.SYSTEM);
            return;
        }

        // 정상 환불은 요청한 사용자의 정보로 기록
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
