package com.gymproject.user.membership.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gymproject.user.membership.domain.entity.UserMembershipHistory;
import com.gymproject.user.membership.domain.type.MembershipChangeType;
import com.gymproject.user.membership.domain.type.MembershipPlanType;
import com.gymproject.user.membership.domain.type.MembershipStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
@Builder
@Schema(description = "멤버십 변경 이력 응답")
public record MembershipHistoryResponse(
        @Schema(description = "이력 ID", example = "10")
        Long historyId,

        @Schema(description = "변경 유형 코드", example = "EXTEND")
        MembershipChangeType changeType,

        @Schema(description = "멤버십 종류 이름", example = "MONTH_1")
        MembershipPlanType planName,

        @Schema(description = "변경 상세 내용 ", example = "멤버십 기간 연장")
        String description,

        @Schema(description = "변동 일수 (양수: 추가, 음수: 차감/환불)", example = "30")
        int changedDays,

        @Schema(description = "변경 전 만료일 (신규 구매시 null)", example = "2026-02-20T00:00:00+09:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Australia/Brisbane")
        OffsetDateTime previousEndDate,

        @Schema(description = "변경 후 만료일 (최종 반영일)", example = "2026-03-22T00:00:00+09:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Australia/Brisbane")
        OffsetDateTime newEndDate,

        @Schema(description = "변경 시점 멤버십 상태", example = "ACTIVE")
        MembershipStatus status,

        @Schema(description = "변경자 (시스템, 관리자, 혹은 본인)", example = "System Admin")
        String modifierName,

        @Schema(description = "처리 일시", example = "2026-01-22T14:30:00+09:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Australia/Brisbane")
        OffsetDateTime createdAt
) {
    // dto 변경 메서드
    public static MembershipHistoryResponse create(UserMembershipHistory entity){
        return MembershipHistoryResponse.builder()
                .historyId(entity.getHistoryId())
                .changeType(entity.getChangeType())
                .planName(entity.getMembershipPlanType())
                .description(entity.getDescription())
                .changedDays(entity.getAmountDays())
                .previousEndDate(entity.getBeforeExpiredAtSnapshot())
                .newEndDate(entity.getAfterExpiredAtSnapshot())
                .status(entity.getStatus())
                .modifierName(entity.getModifierName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
