package com.gymproject.booking.booking.application.dto.reponse;

import com.gymproject.booking.timeoff.domain.type.TimeOffType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "예약 금지 설정 결과 응답")
public record TimeOffResponse(
        @Schema(description = "설정된 블록 고유 ID", example = "5001")
        Long blockId,

        @Schema(description = "금지 시작 시간 (브리즈번 기준)", example = "2026-02-01T13:00:00+11:00")
        OffsetDateTime startAt,

        @Schema(description = "금지 종료 시간 (브리즈번 기준)", example = "2026-02-01T14:00:00+11:00")
        OffsetDateTime endAt,

        @Schema(description = "휴무 타입", example = "LUNCH")
        TimeOffType timeOffType,

        @Schema(description = "사유", example = "점심 시간")
        String reason
) {
}
