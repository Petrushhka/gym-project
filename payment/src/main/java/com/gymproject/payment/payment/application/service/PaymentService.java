package com.gymproject.payment.payment.application.service;

import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.payment.ProductContractV1;
import com.gymproject.common.event.integration.RefundEvent;
import com.gymproject.common.policy.RefundDecision;
import com.gymproject.common.util.GymDateUtil;
import com.gymproject.common.util.JsonSerializer;
import com.gymproject.payment.application.dto.GatewayResponse;
import com.gymproject.payment.application.port.PaymentGatewayPort;
import com.gymproject.payment.payment.application.dto.*;
import com.gymproject.payment.payment.application.service.processor.RefundProcessor;
import com.gymproject.payment.payment.domain.entity.Payment;
import com.gymproject.payment.payment.domain.type.PaymentStatus;
import com.gymproject.payment.payment.exception.PaymentErrorCode;
import com.gymproject.payment.payment.exception.PaymentException;
import com.gymproject.payment.payment.infrastructure.persistence.PaymentRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final JsonSerializer jsonSerializer;
    private final List<RefundProcessor> refundProcessor;
    private final PaymentGatewayPort paymentGatewayPort;

    @Value("${app.payment.base-url}")
    private String apiBaseUrl;

    @Transactional
    // 1. PG사에서 웹훅이 올 때 호출하는 메서드(결제 확정)
    public void capturePayment(PaymentRequest request) {
        //  1. 기존 Pending 상태인 결제 기록 찾기
        Payment payment = getPayment(request);

        // 2. 기존 레코드의 상태를 변경하고 정보를 업데이트
        payment.capture(request.paymentKey());

        paymentRepository.save(payment);
    }

    // 2.  결제 시작(Stripe에 결제 요청) -- Membership 기간연장 로직까지 관여 중
    @Transactional
    public String initiatePayment(InitiatePaymentCommand command) {
        //1. 중복클릭 방지용 (10분 내 동일 상품의 Pending 결제가 있는지 조회
        // PageRequest.of(0,1)로 가장 최근 1건만 가져옴.
        List<Payment> pendingPayment = getPaymentsIn10Minutes(command.userId(), command.productName());

        // 2. 기존 결제가 잇다면, 새 결제창을 만들지 않고 기존 URL 반환
        if (!pendingPayment.isEmpty()) {
            Payment existing = pendingPayment.get(0);
            if (existing.getCheckoutUrl() != null) {
                return existing.getCheckoutUrl();
            }
        }

        // 3. 계약서 스냅샷 생성 및 JSON 파싱
        ProductContractV1 contract = command.toContract();
        String contractSnapshotJson = jsonSerializer.serialize(contract);

        // 4. 결제 엔티티 생성
        Payment newPayment = Payment.pending(
                command.userId(),
                command.amount(),
                command.productName(),
                command.productCode(),
                command.category(),
                contractSnapshotJson
        );

        Payment savedPayment = paymentRepository.save(newPayment);

        // 4. Stripe Session 생성(여기서 메타데이터 넣어야함)
        GatewayResponse response = paymentGatewayPort.createSession(
                savedPayment.getUserId(),
                savedPayment.getPaymentId(),
                savedPayment.getSnapshotProductName(),
                savedPayment.getSnapshotPlanName(),
                savedPayment.getAmountCents(),
                "aud", // 통화 단위
               apiBaseUrl + "/api/v1/payments/payment/success?paymentKey={CHECKOUT_SESSION_ID}",
                apiBaseUrl + "/api/v1/payments/payment/cancel"
        );


        // 5. Stripe에서 발급해준 Id와 URL 기록
        newPayment.updateSessionInfo(response.paymentKey(), response.sessionUrl());

        return response.sessionUrl();
    }

    // 3. 환불 요청
    @Transactional
    public RefundResponse refundRequest(RefundRequest request, UserAuthInfo userAuthInfo) {

        // 1. 결제 확인
        Payment payment = getPaymentByOrderId(request.getPaymentId());

        // 2. 본인 결제 검증
        payment.validateOwnership(userAuthInfo.getUserId());
        payment.validateRefundable();

        // 3. 타입 분기
        RefundProcessor processor = refundProcessor.stream()
                .filter(p -> p.supports(payment.getSnapshotProductCategory()))
                .findFirst()
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.INVALID_PRODUCT_TYPE));

        RefundDecision decision = processor.process(payment, userAuthInfo);

        paymentRepository.save(payment);

        return RefundResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentIntentId(payment.getProviderPaymentId())
                .refundedAt(payment.getUpdatedAt())
                .status(payment.getStatus().name())
                .refundedAmount(decision.refundAmount())
                .build();

    }

    // 4. 환불 완료
    @Transactional
    public void confirmRefund(String paymentIntentId) {
        Payment payment = getPaymentByPaymentKey(paymentIntentId);

        payment.completeRefund();

        paymentRepository.save(payment);

        applicationEventPublisher.publishEvent(
                new RefundEvent(
                        payment.getSnapshotProductCategory().name(),
                        payment.getSourceId(),
                        payment.getContractSnapshot(),
                        payment.getRefundActorId(),
                        payment.getRefundActorRole()
                )
        );
    }

    // 5. 환불 실패
    @Transactional
    public void confirmRefundFailed(String paymentIntent) {
        paymentRepository.findByProviderPaymentId(paymentIntent)
                .ifPresent(payment -> {
                    payment.refundFailed();

                    paymentRepository.save(payment);
                });
    }

    // 6. 결제 자동 취소(내부로직 실패시)
    @Transactional
    public void compensatePayment(String paymentKey){
        // 1. 결제 조회(Long ID로 바로 조회)
        Payment payment = paymentRepository.findByProviderPaymentId(paymentKey).orElseThrow(
                () -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // 2. 검증: 이미 환불되어있는지
        payment.validateRefundable();

        // 3. 프로세서 찾기
        RefundProcessor processor = refundProcessor.stream()
                .filter(p->p.supports(payment.getSnapshotProductCategory()))
                .findFirst()
                .orElseThrow(()->new PaymentException(PaymentErrorCode.INVALID_PRODUCT_TYPE));

        // 4. 실행
        processor.process(payment, UserAuthInfo.system());

        paymentRepository.save(payment);
    }

    // ============ 헬퍼

    private Payment getPaymentByPaymentKey(String paymentIntentId) {
        Payment payment =
                paymentRepository.findByProviderPaymentId(paymentIntentId)
                        .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        return payment;
    }

    private List<Payment> getPaymentsIn10Minutes(Long userId, String productName) {
        List<Payment> pendingPayment = paymentRepository.findRecentPendingPayment(
                userId,
                productName,
                PaymentStatus.PENDING,
                PageRequest.of(0, 1));
        return pendingPayment;
    }

    private Payment getPaymentByOrderId(String orderId) {
        Payment payment = paymentRepository.findByPaymentId(Long.parseLong(orderId))
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        return payment;
    }

    private Payment getPayment(PaymentRequest request) {
        Payment payment = paymentRepository.findByProviderPaymentId(request.paymentKey()) // Key로 찾는 게 더 안전함
                .or(() -> paymentRepository.findById(request.paymentId())) // paymentId로 한번 더 검색
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        return payment;
    }


    // 결제 내역 조회 메서드
    @Transactional(readOnly = true)
    public Page<PaymentResponse> searchPayments(PaymentSearchCondition condition, Pageable pageable) {

        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. ID 검색
            if (condition.paymentId() != null) {
                predicates.add(cb.equal(root.get("paymentId"), condition.paymentId()));
            }

            // 2. 유저 ID 검색 (관리자 조회 or 본인 조회)
            if (condition.userId() != null) {
                predicates.add(cb.equal(root.get("userId"), condition.userId()));
            }

            // 3. 상태 검색
            if (condition.status() != null) {
                predicates.add(cb.equal(root.get("status"), condition.status()));
            }

            // 4. 상품 카테고리 검색
            if (condition.productCategory() != null) {
                predicates.add(cb.equal(root.get("snapshotProductCategory"), condition.productCategory()));
            }

            // 5. 날짜 범위 검색 (호주 시간 적용) ⭐
            if (condition.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        GymDateUtil.convertStartOfDay(condition.startDate())
                ));
            }
            if (condition.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"),
                        GymDateUtil.convertEndOfDay(condition.endDate())
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return paymentRepository.findAll(spec, pageable)
                .map(PaymentResponse::create);
    }
}

