package com.gymproject.user.sesssion.application;

import com.gymproject.user.profile.domain.type.UserSessionStatus;
import com.gymproject.user.sesssion.domain.type.SessionChangeType;
import com.gymproject.user.sesssion.domain.type.SessionProductType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "세션 이력 검색 조건 (모든 필드는 선택사항이며, 비우면 전체 조회됩니다)")
public record SessionHistorySearchCondition(
        @Schema(description = "특정 유저 ID 조회 (관리자용 / 비우면 전체)", example = "10")
        Long userId,

        @Schema(description = "조회 시작 날짜 (YYYY-MM-DD)", example = "2026-01-01")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @Schema(description = "조회 종료 날짜 (YYYY-MM-DD)", example = "2026-12-31")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        @Schema(description = "세션 상태 필터 (ACTIVE, EXPIRED 등)", example = "ACTIVE")
        UserSessionStatus status,

        @Schema(description = "변경 유형 필터 (ISSUE, REFUND 등)", example = "ISSUE")
        SessionChangeType changeType,

        @Schema(description = "멤버십 종류 필터 (MEMBERSHIP_3M 등)" , example = "MEMBERSHIP_3M")
        SessionProductType planType
) {
}
