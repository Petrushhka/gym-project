package com.gymproject.classmanagement.recurrence.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.*;

import static com.gymproject.common.constant.GymTimePolicy.SERVICE_ZONE;

@Getter
@NoArgsConstructor
@Schema(description = "일회성 또는 정기 강좌의 시작 기준 정보")
public class OneTimeClassRequest {

    @NotNull
    @Schema(description = "사용할 수업 템플릿 ID", example = "101")
    private Long classTemplateId;
    // 시작 날짜
    @NotNull
    @Schema(description = "수업 시작 날짜 (브리즈번 기준)", example = "2026-02-01")
    private LocalDate startDate;
    // 시작 시간
    @NotNull
    @Schema(description = "수업 시작 시간", example = "10:00")
    private LocalTime startTime;

    @Schema(description = "타임존 ID (없으면 브리즈번 기본 적용)",
            example = "Australia/Brisbane",
            allowableValues = {"Australia/Brisbane", "Australia/Sydney", "Asia/Seoul"})    private String timezoneId;

    // 시작시간을 기준시간으로 변환
    public OffsetDateTime getStartAt() {
        // 1. timezoneId가 비어있으면 시스템 기본존(SERVICE_ZONE) 사용
        ZoneId zone = (timezoneId == null || timezoneId.isBlank())
                ? SERVICE_ZONE
                : ZoneId.of(timezoneId);

        return ZonedDateTime.of(this.startDate, this.startTime, zone).toOffsetDateTime();
    }

    public OffsetDateTime getEndAt(int durationMinutes) {
        return getStartAt().plusMinutes(durationMinutes);
    }
}
/*
    ROUTINE이지만 딱 한번만 수업이 진행될때 사용하는 DTO임
 */