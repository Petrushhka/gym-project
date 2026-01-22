package com.gymproject.user.sesssion.application;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gymproject.user.profile.domain.type.UserSessionStatus;
import com.gymproject.user.sesssion.domain.entity.UserSessionHistory;
import com.gymproject.user.sesssion.domain.type.SessionChangeType;
import com.gymproject.user.sesssion.domain.type.SessionProductType;
import com.gymproject.user.sesssion.domain.type.SessionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "세션티켓 변경 이력 응답")
public record SessionHistoryResponse(
        @Schema(description = "이력 ID", example = "10")
        Long historyId,

        @Schema(description = "변경 유형", example = "ISSUE")
        SessionChangeType changeType,

        @Schema(description = "세션 종류 이름", example = "PT_20")
        SessionProductType planName,

        @Schema(description = "변동 횟수", example = "-1")
        int changedSessions,

        @Schema(description = "세션권 타입(무료/유료)", example = "FREE_TRAIL")
        SessionType sessionType,

        @Schema(description = "변경 상세 내용", example = "신규 지급")
        String description,

        @Schema(description = "만료일", example = "2026-03-22T00:00:00+09:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Australia/Brisbane")
        OffsetDateTime expiredAt,

        @Schema(description = "변경 시점 상태", example = "ACTIVE")
        UserSessionStatus status,

        @Schema(description = "변경자 (시스템, 관리자, 혹은 본인)", example = "System Admin")
        String modifierName,

        @Schema(description = "처리 일시", example = "2026-01-22T14:30:00+09:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Australia/Brisbane")
        OffsetDateTime createdAt
) {
    // dto 변경 메서드
    public static SessionHistoryResponse create(UserSessionHistory entity){
        return SessionHistoryResponse.builder()
                .historyId(entity.getHistoryId())
                .changeType(entity.getChangeType())
                .planName(entity.getSessionProductType())
                .sessionType(entity.getSessionType())
                .description(entity.getDescription())
                .expiredAt(entity.getExpiredAtSnapshot())
                .status(entity.getStatus())
                .changedSessions(entity.getAmount())
                .modifierName(entity.getModifierName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
