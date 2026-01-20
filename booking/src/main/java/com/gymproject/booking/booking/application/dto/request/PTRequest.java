package com.gymproject.booking.booking.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static com.gymproject.common.constant.GymTimePolicy.SERVICE_ZONE;

@Getter
@NoArgsConstructor
@Schema(description = "1:1 PT 예약 요청")
public class PTRequest {
    @NotNull
    @Schema(description = "트레이너 고유 식별자", example = "8")
    private Long trainerId;

    @NotNull
    @Schema(description = "예약 날짜", example = "2026-02-01")
    private LocalDate date;

    @NotNull
    @Schema(description = "수업 시작 시간", example = "14:00")
    private LocalTime startTime;

    @NotNull
    @Schema(description = "이용권 타입 (FREE_TRIAL, PAID) 등)", example = "FREE_TRIAL")
    private TicketType ticketType;

    @Schema(description = "수업 시간(분)", example = "50")
    private int durationMinutes;

    public OffsetDateTime calculateStartAt(){
        return ZonedDateTime.of(date, startTime, SERVICE_ZONE)
                .toOffsetDateTime();
    }

    public OffsetDateTime calculateEndAt(){
        return calculateStartAt().plusMinutes(durationMinutes);
    }

}
