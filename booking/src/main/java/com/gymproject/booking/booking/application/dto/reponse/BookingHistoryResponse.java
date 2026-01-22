package com.gymproject.booking.booking.application.dto.reponse;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gymproject.booking.booking.domain.entity.BookingHistory;
import com.gymproject.booking.booking.domain.type.BookingActionType;
import com.gymproject.booking.booking.domain.type.BookingStatus;
import com.gymproject.common.security.Roles;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "예약 변경 이력 응답")
public record BookingHistoryResponse(

        @Schema(description = "이력 고유 ID", example = "1052")
        Long historyId,

        @Schema(description = "관련 예약 ID (어떤 예약의 기록인가)", example = "35")
        Long bookingId,

        @Schema(description = "변경자 ID (누가 바꿨는가)", example = "101")
        Long modifierId,

        @Schema(description = "변경자 권한 (MEMBER, TRAINER, SYSTEM)", example = "TRAINER")
        Roles modifierRole,

        @Schema(description = "행동 유형 (CREATE, CANCEL, ATTEND, NOSHOW 등)", example = "CANCEL")
        BookingActionType actionType,

        @Schema(description = "변경 전 상태", example = "CONFIRMED")
        BookingStatus previousStatus,

        @Schema(description = "변경 후 상태", example = "CANCELLED")
        BookingStatus newStatus,

        @Schema(description = "변경 사유", example = "회원의 개인 사정으로 인한 당일 취소 요청")
        String reason,

        // [중요] 아까 정한 규칙대로 '호주 브리즈번 시간'으로 변환해서 내보냄
        @Schema(description = "이력 생성 일시 (호주 브리즈번 시간)", example = "2026-01-22 15:30:00")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Australia/Brisbane")
        OffsetDateTime createdAt
) {
    // Entity -> DTO 변환 메서드 (편의성)
    public static BookingHistoryResponse create(BookingHistory history) {
        return BookingHistoryResponse.builder()
                .historyId(history.getHistoryId())
                .bookingId(history.getBookingId())
                .modifierId(history.getModifierId())
                .modifierRole(history.getModifierRole())
                .actionType(history.getActionType())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .reason(history.getReason())
                .createdAt(history.getCreatedAt()) // 여기서 UTC가 넘어가도 @JsonFormat이 브리즈번 시간으로 바꿔줍니다.
                .build();
    }
}