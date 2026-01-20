package com.gymproject.classmanagement.recurrence.application.dto;

import com.gymproject.classmanagement.recurrence.domain.type.RecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "정기 강좌 개설 요청 정보")
public class RecurrenceClassClassRequest extends OneTimeClassRequest {
    /**
     * repeatDays가 있으면, 연속수업이고, null 값이면 단기성 수업임
     */
    @NotNull(message = "반복 요일을 선택해주세요.")
    @Size(min = 1, message = "반복 요일은 최소 하루 이상이어야 합니다.")
    @Schema(description = "반복 요일 리스트", example = "[\"MONDAY\", \"WEDNESDAY\", \"FRIDAY\"]")
    private List<DayOfWeek> repeatDays;

    @NotNull(message = "수업 유형(CURRICULUM/ROUTINE)은 필수입니다.")
    @Schema(description = "강좌 유형 (CURRICULUM: 중간합류 불가, ROUTINE: 중간합류 가능)", example = "CURRICULUM")
    private RecurrenceType recurrenceType;

    @NotNull(message = "종료 날짜는 필수입니다.")
    @FutureOrPresent(message = "종료 날짜는 과거일 수 없습니다.") // 날짜 검증
    private LocalDate endDate;

}
