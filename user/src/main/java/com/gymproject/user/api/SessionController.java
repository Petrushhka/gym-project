package com.gymproject.user.api;

import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.exception.CommonResDto;
import com.gymproject.user.membership.application.dto.CheckoutResponse;
import com.gymproject.user.sesssion.application.SessionHistoryResponse;
import com.gymproject.user.sesssion.application.SessionHistorySearchCondition;
import com.gymproject.user.sesssion.application.SessionPurchaseRequest;
import com.gymproject.user.sesssion.application.UserSessionService;
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
@RequestMapping("/api/v1/sessions")
public class SessionController {


    private final UserSessionService userSessionService;

    @Operation(summary = "2. 개인수업 세션 티켓 결제 페이지 생성", description = """
            1:1 PT 수업을 이용하기 위한 횟수 기반의 세션 상품의 결제를 생성하고 Stripe 결제 URL을 반환합니다.
            
            1. 사용자가 세션 상품 선택 후 해당 API로 요청
            2. 해당 상품의 정보로 Stripe API와 연동하여 임시 결제 세션을 생성
            3. 반환된 'paymentUrl'로 프론트엔드에서 사용자를 리다이렉트
            4. 결제완료 시 Webhook을 세션 상품 지급(타 API에서 처리)
            
            """)
    @PostMapping("/checkout")
    public ResponseEntity<CommonResDto<CheckoutResponse>> createSession(
            @RequestBody @Valid SessionPurchaseRequest request,
            @AuthenticationPrincipal UserAuthInfo userAuthInfo) {

        // 1. 서비스 호출 검증
        CheckoutResponse response = userSessionService.prepareSessionCheckout(
                userAuthInfo.getUserId(),
                request
        );

        return ResponseEntity.ok(
                CommonResDto.success(200, "결제 페이지 생성 성공", response)
        );
    }

    @Operation(summary = "5. 세션 변경 이력 검색 및 조회", description = """
                다양한 조건으로 세션 이력을 검색합니다.(모든 사용자가 하나의 api 사용)
            
                - 페이징: `page=0&size=20` 형태로 요청(기본값: 0페이지, 10개)
                - 정렬: `sort=createdAt, desc` 최신순
                - 필터: `status=ACTIVE, startDate=2026-01-01` 등 원하는 조건만 파라미터로 추가
            """)
    @GetMapping("/history")
    public ResponseEntity<CommonResDto<Page<SessionHistoryResponse>>> getMembershipHistory(
            // @ParameterObject: Swagger에서 쿼리 파라미터를 펼쳐서 보여줌
            @ParameterObject

            // 1. 검색 조건(쿼리 파라미터)
            @ModelAttribute SessionHistorySearchCondition condition,

            // 2. 페이징 정보(page, size, sort 등)
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,

            @AuthenticationPrincipal UserAuthInfo userAuthInfo

    ) {
        // 회원들은 본인의 기록만 보게 해야함(덮어주는 작업)
        if (!userAuthInfo.isOverTrainer()) {
            condition = new SessionHistorySearchCondition(
                    userAuthInfo.getUserId(),
                    condition.startDate(),
                    condition.endDate(),
                    condition.status(),
                    condition.changeType(),
                    condition.planType()
            );
        }

        Page<SessionHistoryResponse> response = userSessionService.searchSessionHistory(condition, pageable);

        return ResponseEntity.ok(
                CommonResDto.success(200, "이력 검색 성공", response)
        );
    }
}
