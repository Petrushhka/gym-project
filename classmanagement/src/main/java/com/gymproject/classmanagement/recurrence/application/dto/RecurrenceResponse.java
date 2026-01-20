package com.gymproject.classmanagement.recurrence.application.dto;

import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(description = "정기 강좌 개설 결과 응답")
public record RecurrenceResponse(
        @Schema(description = "개설된 강좌 고유 ID", example = "501")
        Long recurrenceId,

        @Schema(description = "강좌 명칭 (템플릿 기반)", example = "8주 완성 다이어트 캠프")
        String title,

        @Schema(description = "총 생성된 수업(스케줄) 횟수", example = "24")
        int totalCreatedCount,

        @Schema(description = "강좌 시작일", example = "2026-02-01")
        LocalDate startDate,

        @Schema(description = "강좌 종료일", example = "2026-03-31")
        LocalDate endDate,

        @Schema(description = "강좌 유형 (CURRICULUM/ROUTINE)", example = "CURRICULUM")
        RecurrenceType recurrenceType
) {
}
