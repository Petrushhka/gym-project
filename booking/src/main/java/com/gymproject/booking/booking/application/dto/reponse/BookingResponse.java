package com.gymproject.booking.booking.application.dto.reponse;

import com.gymproject.booking.booking.domain.type.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "예약 완료 정보 응답")
public record BookingResponse(
        @Schema(description = "예약 고유 ID", example = "12")
        Long bookingId,

        @Schema(description = "트레이너 이름", example = "Jay Trainer")
        String targetName,

        @Schema(description = "수업 시작 시간 (호주/브리즈번 기준)", example = "2026-02-01T14:00:00+11:00")
        OffsetDateTime startAt,

        @Schema(description = "수업 종료 시간 (호주/브리즈번 기준)", example = "2026-02-01T14:50:00+11:00")
        OffsetDateTime endAt,

        @Schema(description = "현재 예약 상태", example = "PENDING")
        BookingStatus status,

        @Schema(description = "사용된 수강권 타입", example = "FREE_TRIAL")
        String sessionType,

        @Schema(description = "사용자에게 보여줄 메세지", example = "트레이너 승인 후 예약이 확정됩니다.")
        String message
) {
    /**
     * 컴팩트 생성자: 객체 생성 시점에 message를 자동으로 결정합니다.
     */
    public BookingResponse {
        // 만약 외부에서 message를 직접 주지 않았다면(null이라면) 상태에 따라 생성
        if (message == null || message.isBlank()) {
            message = switch (status) {
                case PENDING -> "트레이너 승인 후 예약이 확정됩니다.";
                case REJECTED -> "예약을 거절하였습니다.";
                case CONFIRMED -> "예약을 확정하였습니다.";
                case CANCELLED -> "예약을 취소하였습니다.";
                case ATTENDED -> "출석처리 되었습니다.";
                default -> "상태가 업데이트되었습니다.";
            };
        }
    }
}
