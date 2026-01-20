package com.gymproject.booking.booking.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AttendanceRequest {
    @NotNull
    @Schema(description = "현재 위도 (Latitude)", example = "-33.8688")
    private double latitude;

    @NotNull
    @Schema(description = "현재 경도 (Longitude)", example = "151.2093")
    private double longitude;
}
