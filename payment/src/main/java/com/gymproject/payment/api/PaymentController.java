package com.gymproject.payment.api;

import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.exception.CommonResDto;
import com.gymproject.payment.payment.application.dto.RefundRequest;
import com.gymproject.payment.payment.application.dto.RefundResponse;
import com.gymproject.payment.payment.application.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "3. 환불 ", description = "결제 환불 관리")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "1. 결제 환불 요청",description = """
                사용자의 요청에 따라 선택한 상품의 결제된 금액에 대한 환불을 진행합니다.
                
                1. 사용자가 본인의 결제 내역을 가지고 환불을 요청합니다.
                1. 서버에서 상품의 환불 가능 여부 및 환불 정책에 따른 금액을 정합니다.
                2. Stripe API를 호출하여 실제 결제 취소(Refund)를 수행합니다.
                3. 환불 성공 시 관련 멤버십/세션권 회수하거나 상태 업데이트를 합니다.
                
            """)
    @PostMapping("/refund")
    public ResponseEntity<CommonResDto<RefundResponse>> refund(@RequestBody RefundRequest request,
                                                               @AuthenticationPrincipal UserAuthInfo userAuthInfo){

        RefundResponse response = paymentService.refundRequest(request, userAuthInfo);

        return ResponseEntity.ok(
                CommonResDto.success(200, "환불이 정상적으로 처리되었습니다.", response)
        );

    }


    @Operation(
            summary = "2. 결제 성공 리다이렉트 경로",
            description = "Stripe 결제 창에서 결제가 완료된 후 사용자가 돌아오는 경로입니다. 추후 프론트엔드의 '결제 완료' 페이지로 연결됩니다."
    )
    @GetMapping("/payment/success")
    public String success() {
        return "결제 완료, 로그 확인";
    }

    @Operation(
            summary = "3. 결제 취소 리다이렉트 경로",
            description = "사용자가 Stripe 결제 창에서 결제를 취소하거나 뒤로 가기를 눌렀을 때 돌아오는 경로입니다."
    )
    @GetMapping("/payment/cancel")
    public String cancel() {
        return "결제 취소, 로그 확인";
    }
}
