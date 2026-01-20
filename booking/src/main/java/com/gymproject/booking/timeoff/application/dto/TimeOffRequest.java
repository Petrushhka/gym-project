package com.gymproject.booking.timeoff.application.dto;

import com.gymproject.booking.timeoff.domain.type.TimeOffType;
import com.gymproject.booking.timeoff.exception.TimeOffErrorCode;
import com.gymproject.booking.timeoff.exception.TimeOffException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static com.gymproject.common.constant.GymTimePolicy.SERVICE_ZONE;

@Getter
@NoArgsConstructor
@Schema(description = "트레이너 예약 금지 시간 등록 요청 ")
public class TimeOffRequest {

    // ISO 8601 포맷: yyyy-MM-ddTHH:mm:ss+09:00, 날짜 시간은 분리해서 프론트입장에서 편함
    @NotNull
    @Schema(description = "시작 날짜", example = "2026-02-01")
    private LocalDate startDate;

    @NotNull
    @Schema(description = "시작 시간", example = "13:00")
    private LocalTime startTime;

    @NotNull
    @Schema(description = "종료 날짜", example = "2026-02-01")
    private LocalDate endDate;

    @NotNull
    @Schema(description = "종료 시간", example = "14:00")
    private LocalTime endTime;

    @NotNull
    @Schema(description = "휴무 타입 (VACATION, BREAK, PERSONAL, ETC)", example = "PERSONAL")
    private TimeOffType timeOffType;

    @Size(max = 255)
    @Schema(description = "휴무 상세 사유", example = "점심 시간 및 휴식")
    private String reason; // 휴무 사유 (예: 점심 시간, 개인 일정)

    public OffsetDateTime getStartDateTime() {
        ZonedDateTime startAt = ZonedDateTime.of(startDate, startTime, SERVICE_ZONE);

        return startAt.toOffsetDateTime();
    }

    public OffsetDateTime getEndDateTime() {
        ZonedDateTime endAt = ZonedDateTime.of(endDate, endTime, SERVICE_ZONE);

        return endAt.toOffsetDateTime();
    }

    public void validateConflict(OffsetDateTime startAt,
                                 OffsetDateTime endAt,
                                 OffsetDateTime now){
        if (startAt.isBefore(now)) {
            throw new TimeOffException(TimeOffErrorCode.INVALID_START_TIME);
        }
        if(startAt.isAfter(endAt) || startAt.isEqual(endAt)){
            throw new TimeOffException(TimeOffErrorCode.INVALID_TIME_RANGE);
        }
    }
}
