package com.gymproject.classmanagement.recurrence.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
@Schema(description = "일회성 그룹 수업 개설 결과 응답")
public record OneTimeClassResponse(
        @Schema(description = "생성된 수업(스케줄) 고유 ID", example = "7001")
        Long scheduleId,

        @Schema(description = "수업 제목", example = "토요일 전신 유산소 특강")
        String title,

        @Schema(description = "수업 시작 시간 (브리즈번 기준)", example = "2026-02-07T10:00:00+10:00")
        OffsetDateTime startAt,

        @Schema(description = "수업 종료 시간 (브리즈번 기준)", example = "2026-02-07T11:00:00+10:00")
        OffsetDateTime endAt,

        @Schema(description = "수업 상태", example = "OPEN")
        String status,

        @Schema(description = "수업 정원", example = "15")
        int capacity
) {

}