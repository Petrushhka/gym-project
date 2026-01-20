package com.gymproject.classmanagement.template.application.dto;

import com.gymproject.classmanagement.template.domain.type.ClassKind;
import com.gymproject.classmanagement.template.domain.type.RecommendLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@Schema(description = "수업 템플릿 생성 요청 DTO")
public class TemplateRequest {

    @NotBlank(message = "수업 제목은 필수입니다.")
    @Schema(description = "수업 제목", example = "[초급] 오전 타바타 루틴")
    private String title;

    @Schema(description = "수업 상세 설명", example = "초보자를 위한 고강도 인터벌 트레이닝입니다.")
    private String description;

    @Min(value = 1, message = "정원은 최소 1명 이상이어야 합니다.")
    @Schema(description = "수업 정원", example = "10")
    int capacity;

    @Min(value = 10, message = "수업 시간은 최소 10분 이상이어야 합니다.")
    @Schema(description = "수업 소요 시간 (분 단위)", example = "50")
    private int durationMinutes;

    @Schema(description = "추천 난이도 (BEGINNER, INTERMEDIATE, ADVANCED)", example = "BEGINNER")
    private RecommendLevel recommendLevel;

    @Schema(description = "수업 종류 (PERSONAL, GROUP_ROUTINE, GROUP_CURRICULUM)", example = "GROUP_ROUTINE")
    private ClassKind classKind;

}
