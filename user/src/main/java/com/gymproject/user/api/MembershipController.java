package com.gymproject.user.api;

import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.exception.CommonResDto;
import com.gymproject.user.membership.application.UserMembershipService;
import com.gymproject.user.membership.application.dto.CheckoutResponse;
import com.gymproject.user.membership.application.dto.MembershipPurchaseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2. 결제", description = "상품 결제 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/memberships")
public class MembershipController {

    private final UserMembershipService userMembershipService;

    @Operation(summary = "1. 멤버십 결제 페이지 생성", description = """
            선택한 멤버십 상품에 대한 결제 세션을 생성하고 Stripe 결제 페이지 URL을 반환합니다.
            
            1. 사용자의 멤버십 상품 선택 후 해당 API로 요청
            2. 해당 상품의 정보로 Stripe API와 연동하여 임시 결제 세션을 생성
            3. 반환된 'paymentUrl'로 프론트엔드에서 사용자를 리다이렉트
            4. 결제완료 시 Webhook을 통해 멤버십 상품 지급(타 API에서 처리)
            
            """)
    @PostMapping("/checkout")
    public ResponseEntity<CommonResDto<CheckoutResponse>> createMembership(
            @RequestBody @Valid MembershipPurchaseRequest request,
            @AuthenticationPrincipal UserAuthInfo userAuthInfo){

        // 1. 서비스 호출(검증 - 날짜계산 -> Payment 요청)
        CheckoutResponse response = userMembershipService.prepareMembershipCheckout(
                userAuthInfo.getUserId(),
                request
        );

        return ResponseEntity.ok(
                CommonResDto.success(200,"결제 페이지 생성 성공", response)
        );
    }

}
