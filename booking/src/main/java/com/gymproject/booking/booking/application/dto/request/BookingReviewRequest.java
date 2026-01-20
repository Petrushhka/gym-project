package com.gymproject.booking.booking.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "트레이너 예약 승인/거절 요청")
public class BookingReviewRequest {

    public enum Type {
        CONFIRM, REJECT
    }

    @NotNull
    @Schema(description = "변경할 예약 상태 (CONFIRMED 또는 REJECTED)", example = "CONFIRMED")
    private Type type;

//    @Schema(description = "거절 사유 (상태가 REJECTED일 경우 필수 권장)", example = "해당 시간에 개인 일정이 있습니다.")
//    private String reviewNote;
}
