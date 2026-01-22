package com.gymproject.user.api;

import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.exception.CommonResDto;
import com.gymproject.user.membership.application.UserMembershipService;
import com.gymproject.user.membership.application.dto.CheckoutResponse;
import com.gymproject.user.membership.application.dto.MembershipHistoryResponse;
import com.gymproject.user.membership.application.dto.MembershipHistorySearchCondition;
import com.gymproject.user.membership.application.dto.MembershipPurchaseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "4. 멤버십 변경 이력 검색 및 조회", description = """
                다양한 조건으로 멤버십 이력을 검색합니다.(모든 사용자가 하나의 api 사용)
                
                - 페이징: `page=0&size=20` 형태로 요청(기본값: 0페이지, 10개)
                - 정렬: `sort=createdAt, desc` 최신순
                - 필터: `status=ACTIVE, startDate=2026-01-01` 등 원하는 조건만 파라미터로 추가
            """)
    @GetMapping("/history")
    public ResponseEntity<CommonResDto<Page<MembershipHistoryResponse>>> getMembershipHistory(
            // @ParameterObject: Swagger에서 쿼리 파라미터를 펼쳐서 보여줌
            @ParameterObject
            // 1. 검색 조건(쿼리 파라미터)
            @ModelAttribute MembershipHistorySearchCondition condition,

            // 2. 페이징 정보(page, size, sort 등)
            @PageableDefault(size = 10, sort="createdAt", direction = Sort.Direction.DESC) Pageable pageable,

            @AuthenticationPrincipal UserAuthInfo userAuthInfo

    ){
        // 회원들은 본인의 기록만 보게 해야함(덮어주는 작업)
        if(!userAuthInfo.isOverTrainer()) {
            condition = new MembershipHistorySearchCondition(
                    userAuthInfo.getUserId(),
                    condition.startDate(),
                    condition.endDate(),
                    condition.status(),
                    condition.changeType(),
                    condition.planType()
            );
        }

        Page<MembershipHistoryResponse> response = userMembershipService.searchMembershipHistory(condition, pageable);

        return ResponseEntity.ok(
                CommonResDto.success(200 , "이력 검색 성공", response)
        );
    }
}
