package com.gymproject.payment.application.port;

import com.gymproject.payment.application.dto.GatewayResponse;
import com.stripe.Stripe;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// StripeClient가 어댑터 모양을 쓴거임.
@Component
@RequiredArgsConstructor
public class StripePaymentAdapter implements PaymentGatewayPort {

    @Value("${stripe.api-secret-key}")
    private String apiSecretKey;

    // 프로젝트가 시작될 때 Stripe API를 설정
    @PostConstruct
    public void init() {
        Stripe.apiKey = apiSecretKey;
    }

    @Override
    public GatewayResponse createSession(
            Long userId, Long paymentId, String productName,
            String planName, Long amount,
            String currency,
            String successUrl, String cancelUrl) {

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    // 결제 성공, 취소 시 돌아올 URL (프론트엔드 주소)
                    .setSuccessUrl("http://localhost:8080/api/v1/payments/payment/success") // 프론트엔드 주소임
                    .setCancelUrl("http://localhost:8080/api/v1/payments/payment/cancel") // 프론트엔드 주소임
                    .setClientReferenceId(String.valueOf(userId)) // UserId 저장

                    // 메타데이터
                    .putMetadata("paymentId", String.valueOf(paymentId))
                    .putMetadata("planName", planName)

                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("aud")
                                                    .setUnitAmount(amount)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(productName)
                                                                    .build()
                                                    ).build()
                                    ).build())
//                    .putMetadata("startDate", checkOutRequest.getStartDate().toString()) << [중요] Stripe에 모든 정보를 주지 말자
                    .build();

            // Session.create을 부르는 순간 Stripe SDK 가 내부적으로 HTTP 통신으로 요청을 날림
            Session session = Session.create(params);

            return new GatewayResponse(session.getUrl(), session.getId());

        } catch (Exception e) {
            throw new RuntimeException("Stripe 세션 생성 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public void refund(String paymentKey, Long amount) {
        try {
            RefundCreateParams.Builder builder =
                    RefundCreateParams.builder()
                            .setPaymentIntent(paymentKey);

            // 부분 환불일 때 만 amount 세팅
            if (amount > 0) {
                builder.setAmount(amount); // cents
            }
            Refund.create(builder.build());
        } catch (Exception e) {
            throw new RuntimeException("Stripe 환불 요청 실패,", e);
        }
    }
}

