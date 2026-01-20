package com.gymproject.payment.api;

import com.gymproject.common.dto.exception.CommonResDto;
import com.gymproject.payment.payment.application.dto.PaymentRequest;
import com.gymproject.payment.payment.application.service.PaymentService;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/*
  wehhook에다가는 에러나 예외를 던지면 안됨!
  2xx 아니면 무한으로 재시도하기 때문임

  [중요]
  Webhook은 순서를 보장하지않음!!
  따라서 이벤트 타입 + 현재 상태(paymentStatus)를 가지고 잘 막아줘야함.
  stripe가 갑자기 중단됐다가 재시작하면서 웹훅의 순서가 뒤집혀서 오거나,
 사용자가 결제후 재빨리 환불을 누르는 등.
 */
@Tag(name = "2. 결제", description = "상품 결제 관리")
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {

    Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook-signing-secret-key}")
    private String endpointSecret;

    private final PaymentService paymentService;

    @Operation(
            summary = "3. Stripe Webhook 수신 엔드포인트",
            description = """
                    Stripe 서버로부터 결제 및 환불 관련 이벤트를 수신합니다.
                    
                    1. Signature 검증: Stripe-Signature 헤더를 통해 발신처가 Stripe임을 보장합니다.
                    2. 멱등성 보장: 동일한 이벤트가 여러 번 수신되어도 DB 상태가 안전하게 유지되도록 설계되었습니다.
                    3. 무조건 200 OK: 비즈니스 로직 에러가 발생하더라도 Stripe의 불필요한 재시도를 방지하기 위해 200 응답을 원칙으로 하며, 에러는 서버 로그로 추적합니다.
                    """
    )
    @PostMapping("/stripe")
    public ResponseEntity<CommonResDto<?>> handleStripeWebhook(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Stripe에서 보낸 원본 JSON payload",
                    required = true,
            content = @Content(examples = @ExampleObject(value = "{\"id\": \"evt_123\", \"type\": \"checkout.session.completed\"}")))
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            // Stripe에서 발신한 건지 확인
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            log.error("Webhook 서명 검증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event); // 결제 성공
            case "charge.refunded" -> handleRefundCompleted(event); // 환불
            case "charge.refund.updated" -> handleRefundFailed(event); // 환불 실패
            default -> log.debug("미처리 이벤트 타입: {}", event.getType());
        }
        return ResponseEntity.ok(CommonResDto.success(200, "Webhook Processed", null));
    }


    private void handleCheckoutCompleted(Event event) {
        // 데이터 꺼내기 (Stripe 객체로 변환)
        Session session = (Session) event.getDataObjectDeserializer().getObject()
                .orElse(null);

        if (session != null) {
            // Metadata에 심어두었던 paymentId를 꺼내기.
            String metadataPaymentId = session.getMetadata().get("paymentId");

            try {
                PaymentRequest request = new PaymentRequest(
                        Long.valueOf(session.getClientReferenceId()), //userId
                        null, // DB 또는 metaData에서 추출
                        session.getAmountTotal(), // 결제 금액
                        session.getPaymentIntent(), // pg사 결제 고유번호
                        Long.valueOf(metadataPaymentId) // db 속 PaymentId
                );
                log.info("결제 성공 Wehbook 수신, UserID: {}", session.getClientReferenceId());

                // 서비스로직 호출(Pending -> Captured)
                paymentService.capturePayment(request);

            } catch (Exception e) {
                log.error("Webhook 처리 중 비지니스 로직 오류; {}", e.getMessage());
            }
        }
    }

    private void handleRefundCompleted(Event event) {
        // [중요] 공부
        Charge charge = (Charge) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);
        if (charge != null) {
            log.info("환불 완료 확인 (PaymentIntent ID: {})", charge.getPaymentIntent());
        }
        String paymentIntentId = charge.getPaymentIntent();

        paymentService.confirmRefund(paymentIntentId);
    }

    private void handleRefundFailed(Event event) {
        // [수정] charge.refund.updated 이벤트는 Refund 객체를 반환
        Refund refund = (Refund) event.getDataObjectDeserializer()
                .getObject()
                .orElse(null);

        if (refund == null) return;

        //  Refund 객체이므로 리스트를 뒤질 필요 없이 바로 상태 확인 가능
        if ("failed".equals(refund.getStatus())) {
            log.warn("환불 실패 웹훅 수신: RefundID={}, PI={}", refund.getId(), refund.getPaymentIntent());
            paymentService.confirmRefundFailed(refund.getPaymentIntent());
        }
    }

}


