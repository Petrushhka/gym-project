package com.gymproject.user.membership.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

@Schema(description = "멤버십 결제 페이지 생성 요청")
public record MembershipPurchaseRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        @Schema(description = "구매할 상품 고유 ID", example = "55")
        Long productId, // Payment 모듈에 등록된 상품의 ID( 예: 55)

        @NotBlank(message = "구매 타입은 필수 입니다.")
        @Schema(description = "구매 유형 (NEW: 신규 가입, EXTEND: 기존 멤버십 연장)", example = "NEW")
        String type, // NEW(신규) or EXTEND (연장)

        @Schema(description = "멤버십 희망 시작일 (ISO-8601 형식, 미입력 시 결제 완료 즉시 시작)",
                example = "2026-02-01T00:00:00+10:00")
        OffsetDateTime startDate // 희망 시작일
) {
}
