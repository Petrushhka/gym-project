package com.gymproject.booking.booking.application.dto.request;

import com.gymproject.booking.booking.domain.type.BookingActionType;
import com.gymproject.booking.booking.domain.type.BookingStatus;
import com.gymproject.booking.booking.domain.type.BookingType;
import com.gymproject.common.security.Roles;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "예약 이력 검색 조건")
public record BookingHistorySearchCondition(

        @Schema(description = "특정 예약 번호로 조회 (특정 예약의 이력만 보고 싶을 때)", example = "105")
        Long bookingId,

        @Schema(description = "변경자 ID (누가 변경했는지 조회)", example = "1")
        Long modifierId,

        @Schema(description = "변경 유형 필터 (CREATE, CANCEL, ATTEND, NOSHOW, REJECT, CONFIRM)", example = "CANCEL")
        BookingActionType actionType,

        @Schema(description = "조회 시작 날짜 (YYYY-MM-DD)", example = "2026-01-01")
        @DateTimeFormat(pattern = "yyyy-MM-dd") // 쿼리 파라미터 String -> LocalDate 자동 변환
        LocalDate startDate,

        @Schema(description = "조회 종료 날짜 (YYYY-MM-DD)", example = "2026-01-31")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,

        @Schema(description = "변경자 권한 (MEMBER, TRAINER, SYSTEM)", example = "SYSTEM")
        Roles modifierRole,

        @Schema(description = "수업 종류 (PERSONAL, GROUP_ROUTINE)", example = "PERSONAL")
        BookingType bookingType,

        @Schema(description = "최종 상태 (CONFIRMED, CANCELLED)", example = "CANCELLED")
        BookingStatus status
) {}