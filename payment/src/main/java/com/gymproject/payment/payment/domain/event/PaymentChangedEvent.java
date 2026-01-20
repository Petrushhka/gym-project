package com.gymproject.payment.payment.domain.event;

import com.gymproject.payment.payment.domain.type.PaymentStatus;
import com.gymproject.payment.payment.domain.entity.Payment;
import lombok.Getter;

@Getter
public class PaymentChangedEvent {

    private final Payment payment;
    private final PaymentStatus previousStatus;
    private final PaymentStatus newStatus;
    private final String reason;

    public PaymentChangedEvent(Builder builder) {
        this.payment = builder.payment;
        this.previousStatus = builder.previousStatus;
        this.newStatus = builder.newStatus;
        this.reason = builder.reason;
    }

    public static class Builder {
        private final Payment payment;
        private PaymentStatus previousStatus;
        private PaymentStatus newStatus;
        private String reason;

        /*
            내가 조합을 어떻게 할 것인지 생각을 해보고 Builder 패턴 만들어야함
         */
        public Builder(Payment payment) {
            this.payment = payment;
        }

        public Builder  previousStatus(PaymentStatus previousStatus) {
            this.previousStatus = previousStatus;
            return this;
        }

        public Builder  newStatus(PaymentStatus newStatus) {
            this.newStatus = newStatus;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public PaymentChangedEvent build() {
            return new PaymentChangedEvent(this);
        }
    }

    // 정적 팩토리 메서드

    // 결제 완료
    public static PaymentChangedEvent captured(Payment payment) {
        return new Builder(payment)
                .previousStatus(PaymentStatus.PENDING)
                .newStatus(PaymentStatus.CAPTURED)
                .reason("결제 승인 완료")
                .build();
    }

    // 결제 실패
    public static PaymentChangedEvent failed(Payment payment, String reason) {
        return new Builder(payment)
                .previousStatus(payment.getStatus())
                .newStatus(PaymentStatus.FAILED)
                .reason(reason)
                .build();
    }


}

/*
   해당 이벤트는 결제의 상태가 바뀔 때마다 발생함.
 */
