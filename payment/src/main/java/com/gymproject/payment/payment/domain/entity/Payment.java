package com.gymproject.payment.payment.domain.entity;

import com.gymproject.common.security.Roles;
import com.gymproject.common.util.GymDateUtil;
import com.gymproject.payment.payment.domain.event.PaymentChangedEvent;
import com.gymproject.payment.payment.domain.type.PaymentStatus;
import com.gymproject.payment.payment.exception.PaymentErrorCode;
import com.gymproject.payment.payment.exception.PaymentException;
import com.gymproject.payment.product.domain.type.ProductCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.OffsetDateTime;

/**
 * [중요]!!!!!
 * 외부 결제 결과를 사실 그대로 기록하는 엔터티임 (따라서 product를 ManyToOne 처럼 두지 않음)
 * 결제가 성공하면 결제의 종류에 따라 이벤트를 발행하여
 * 해당 도메인에서 상태를 변경하는 것이 핵심.,
 * 다시 말해, Payment는 돈과 관련된 사실을 보존하는 도메인인 것임.
 * 1) 얼마를 결제?
 * 2) 어떤 PG에서 결제?
 * 3) 실제 결제가 성공했는지? 실패했는지?
 * 4) 무엇에 대한 결제인지? (이용기간권, 유저세션권)
 */
@Getter
@Entity
@Table(name = "PAYMENT_RECORD_TB")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends AbstractAggregateRoot<Payment> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "provider_payment_id", nullable = true)
    /// Stipe API를 호출하기 전에는 ID를 알 수 없음.
    private String providerPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    // ============== 상품 스냅샷

    @Column(name = "snapshot_product_name", nullable = false)
    private String snapshotProductName; // "여름 할인 1개월권"

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_product_type", nullable = false)
    private ProductCategory snapshotProductCategory; // MEMBERSHIP or SESSION

    @Column(name = "snapshot_plan_name", nullable = false)
    private String snapshotPlanName; // "MONTH_1" or "PT_10"

    @Column(name = "contract_snapshot", columnDefinition = "jsonb", nullable = false)
    private String contractSnapshot; // 세부 결제 내용(멤버십 시작일 등)

    // ========== 부가 정보

    // 생성된 결제창 URL 저장(중복 클릭 시 다시 보내줄 URL)
    @Column(name = "checkout_url")
    private String checkoutUrl; // 재시도 URL


    @Column(name = "refund_actor_id")
    private Long refundActorId;

    @Column(name = "refund_actor_role")
    @Enumerated(EnumType.STRING)
    private Roles refundActorRole;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // 호주시간으로 저장
    @PrePersist
    public void onPrePersist() {
        OffsetDateTime now = GymDateUtil.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // 호주시간으로 저장
    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = GymDateUtil.now();
    }

    @Builder
    public Payment(Long userId, Long amountCents, String providerPaymentId,
                   Long sourceId,
                   PaymentStatus status, String snapshotProductName,
                   ProductCategory snapshotProductCategory, String snapshotPlanName,
                   String contractSnapshot) {
        this.userId = userId;
        this.sourceId = sourceId;
        this.amountCents = amountCents;
        this.providerPaymentId = providerPaymentId;
        this.status = status;
        this.snapshotProductName = snapshotProductName;
        this.snapshotProductCategory = snapshotProductCategory;
        this.snapshotPlanName = snapshotPlanName;
        this.contractSnapshot = contractSnapshot;
    }

    // 1. 처음 결제 생성 (Pending 상태)
    public static Payment pending(Long userId, Long price,
                                  String productName,
                                  String productCode,
                                  ProductCategory productCategory,
                                  String contractSnapshot) {
        return Payment.builder()
                .userId(userId)
                .amountCents(price)
                .status(PaymentStatus.PENDING)
                .snapshotProductName(productName)
                .snapshotPlanName(productCode)
                .snapshotProductCategory(productCategory)
                .contractSnapshot(contractSnapshot)
                .build();
    }

    /// ----------- 상태 및 검증

    // 2. 세션 정보 업데이트(Stripe API 호출 후)
    public void updateSessionInfo(String providerPaymentId, String checkoutUrl) {
        this.providerPaymentId = providerPaymentId; // 3party 결제세션 고유 ID
        this.checkoutUrl = checkoutUrl;
    }

    // 3. 결제 확정 캡쳐 로직
    public void capture(String pgPaymentKey) {
        if (this.status == PaymentStatus.CAPTURED) { // 결제확정된 건 무시
            return;
        }
        if (this.status != PaymentStatus.PENDING) { // Pending상태에서만 확정이 가능함
            throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
        }

        this.status = PaymentStatus.CAPTURED;
        this.providerPaymentId = pgPaymentKey;

        this.registerEvent(PaymentChangedEvent.captured(this));
    }

    // 4. 환불 요청(아직 이벤트 x)
    public void refundRequested(Long refundActorId,
                                Roles refundActorRole) {
        validateRefundable();

        this.status = PaymentStatus.REFUND_REQUEST;
        this.refundActorId = refundActorId;
        this.refundActorRole = refundActorRole;
    }

    // 5. 환불 완료 (PG 처리 후)
    public void completeRefund() {
        // 이미 환불 완료 상태라면 무시[멱등성]
        if(this.status == PaymentStatus.REFUNDED) {
            return;
        }
        // Capture일 때도 가능함.(시스템 에러인 상황에서 환불이 바로 일어날 수 있기 때문임)[주의]
        if (this.status != PaymentStatus.REFUND_REQUEST &&
        this.status != PaymentStatus.CAPTURED) {
            throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
        }
        this.status = PaymentStatus.REFUNDED;
    }

    // 6. 환불실패 (REQUEST로 멈춰있으면 안되니까)
    public void refundFailed() {
        if (this.status == PaymentStatus.REFUND_REQUEST) {
            this.status = PaymentStatus.REFUND_FAILED;
        }
    }

    // 7. 소스 ID 바인딩 (상품id)를 추후로 받아오기때문에 바인딩해야함.
    public void bindSourceId(Long sourceId) {
        if (this.sourceId != null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_BOUND);
        }

        this.sourceId = sourceId;
    }


    public void validateOwnership(Long requesterId) {
        if (!this.userId.equals(requesterId)) {
            throw new PaymentException(PaymentErrorCode.ACCESS_DENIED);
        }
    }

    public void validateRefundable() {
        if (this.status != PaymentStatus.CAPTURED ) {
            throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS);
        }
    }


}


/*
        sourceId 추가
1. Payment PENDING 생성
2. Stripe 결제
3. Payment CAPTURED
4. PaymentChangedEvent 발행
5. Membership / Session 생성
6. 생성된 엔티티 ID를 Payment에 sourceId로 연결
 */