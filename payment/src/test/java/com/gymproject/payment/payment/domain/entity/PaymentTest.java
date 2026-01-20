package com.gymproject.payment.payment.domain.entity;

import com.gymproject.common.security.Roles;
import com.gymproject.payment.payment.domain.entity.util.DomainEventsTestUtils;
import com.gymproject.payment.payment.domain.event.PaymentChangedEvent;
import com.gymproject.payment.payment.domain.type.PaymentStatus;
import com.gymproject.payment.payment.exception.PaymentException;
import com.gymproject.payment.product.domain.type.ProductCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {
    @Nested
    @DisplayName("1. 결제 생성 및 초기화 테스트")
    class CreateTest {

        @Test
        @DisplayName("pending 메서드로 생성 시 초기 상태는 PENDING이며 주요 필드가 설정된다")
        void create_pending_success() {
            // given
            Long userId = 1L;
            Long amount = 50000L;
            String productName = "PT 10회";
            String productCode = "PT_10";
            String contractSnapshot = "{\"days\": 30}";

            // when
            Payment payment = Payment.pending(
                    userId, amount, productName, productCode,
                    ProductCategory.SESSION, contractSnapshot
            );

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getAmountCents()).isEqualTo(amount);
            assertThat(payment.getSnapshotProductName()).isEqualTo(productName);
            assertThat(payment.getSnapshotProductCategory()).isEqualTo(ProductCategory.SESSION);
            assertThat(payment.getProviderPaymentId()).isNull(); // 아직 PG 연동 전
        }
    }

    @Nested
    @DisplayName("2. 결제 확정(Capture) 로직 테스트")
    class CaptureTest {

        @Test
        @DisplayName("Pending 상태에서 Capture 성공 시 상태가 CAPTURED로 변경되고 이벤트가 발행된다")
        void capture_success() {
            // given
            Payment payment = createPayment(PaymentStatus.PENDING);
            String pgKey = "pg_key_1234";

            // when
            payment.capture(pgKey);

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(payment.getProviderPaymentId()).isEqualTo(pgKey);

            // [중요] AbstractAggregateRoot를 통해 이벤트가 등록되었는지 확인
            List<Object> events = DomainEventsTestUtils.getEvents(payment);
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(PaymentChangedEvent.class);
        }

        @Test
        @DisplayName("이미 Captured 된 상태에서 호출 시 아무 일도 일어나지 않는다 (멱등성 보장)")
        void capture_idempotency() {
            // given
            Payment payment = createPayment(PaymentStatus.CAPTURED);
            String pgKey = "pg_key_1234";

            // when
            payment.capture(pgKey);

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            // 이미 처리된 건이라 이벤트 추가 발행 안 함 (로직에 따라 다를 수 있으나 현재 코드는 return 처리됨)
        }

        @Test
        @DisplayName("Pending 상태가 아닐 때(예: 환불됨) Capture 시도 시 예외 발생")
        void capture_fail_invalid_status() {
            // given
            Payment payment = createPayment(PaymentStatus.REFUNDED);

            // when & then
            assertThatThrownBy(() -> payment.capture("pg_key"))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_INVALID_STATUS");
        }
    }

    @Nested
    @DisplayName("3. 환불 프로세스 테스트")
    class RefundTest {

        @Test
        @DisplayName("Captured 상태에서 환불 요청 시 상태가 REFUND_REQUEST로 변경된다")
        void refund_request_success() {
            // given
            Payment payment = createPayment(PaymentStatus.CAPTURED);

            // when
            payment.refundRequested(100L, Roles.MEMBER);

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_REQUEST);
            assertThat(payment.getRefundActorId()).isEqualTo(100L);
            assertThat(payment.getRefundActorRole()).isEqualTo(Roles.MEMBER);
        }

        @Test
        @DisplayName("Captured 상태가 아닐 때(예: Pending) 환불 요청 시 예외 발생")
        void refund_request_fail() {
            // given
            Payment payment = createPayment(PaymentStatus.PENDING);

            // when & then
            assertThatThrownBy(() -> payment.refundRequested(100L, Roles.MEMBER))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_INVALID_STATUS");
        }

        @Test
        @DisplayName("환불 요청 상태에서 환불 완료 처리 시 상태가 REFUNDED로 변경된다")
        void complete_refund_success() {
            // given
            Payment payment = createPayment(PaymentStatus.REFUND_REQUEST);

            // when
            payment.completeRefund();

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("환불 실패 처리 시 상태가 REFUND_FAILED로 변경된다")
        void refund_failed_success() {
            // given
            Payment payment = createPayment(PaymentStatus.REFUND_REQUEST);

            // when
            payment.refundFailed();

            // then
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_FAILED);
        }
    }

    @Nested
    @DisplayName("4. 데이터 바인딩 및 검증 테스트")
    class BindingAndValidationTest {

        @Test
        @DisplayName("SourceId(상품ID) 바인딩 성공")
        void bind_source_id_success() {
            // given
            Payment payment = createPayment(PaymentStatus.CAPTURED); // sourceId가 null인 상태

            // when
            payment.bindSourceId(777L);

            // then
            assertThat(payment.getSourceId()).isEqualTo(777L);
        }

        @Test
        @DisplayName("SourceId가 이미 존재하는데 다시 바인딩 하려 하면 예외 발생")
        void bind_source_id_fail_already_exists() {
            // given
            Payment payment = createPayment(PaymentStatus.CAPTURED);
            payment.bindSourceId(777L); // 1차 바인딩

            // when & then
            assertThatThrownBy(() -> payment.bindSourceId(888L))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_ID_NULL");
        }

        @Test
        @DisplayName("본인 결제 검증 - 본인이면 통과")
        void validate_ownership_success() {
            // given
            Payment payment = createPayment(PaymentStatus.CAPTURED); // userId=1L

            // when & then (예외가 발생하지 않아야 함)
            payment.validateOwnership(1L);
        }

        @Test
        @DisplayName("본인 결제 검증 - 타인이면 예외 발생")
        void validate_ownership_fail() {
            // given
            Payment payment = createPayment(PaymentStatus.CAPTURED); // userId=1L

            // when & then
            assertThatThrownBy(() -> payment.validateOwnership(999L))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "PAYMENT_ACCESS_DENIED");
        }
    }

    // --- 테스트용 헬퍼 메서드 ---
    private Payment createPayment(PaymentStatus status) {
        return Payment.builder()
                .userId(1L)
                .amountCents(10000L)
                .status(status)
                .snapshotProductName("테스트 상품")
                .snapshotPlanName("TEST_CODE")
                .snapshotProductCategory(ProductCategory.MEMBERSHIP)
                .contractSnapshot("{}")
                .build();
    }
}