package com.gymproject.classmanagement.template.application.dto;

import com.gymproject.classmanagement.template.domain.entity.Template;
import com.gymproject.classmanagement.template.domain.type.ClassKind;
import com.gymproject.classmanagement.template.domain.type.RecommendLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;



@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "수업 템플릿 생성 결과 응답")
public class TemplateResponse {

    @Schema(description = "생성된 템플릿 고유 ID", example = "101")
    private Long templateId;

    @Schema(description = "수업 제목", example = "[초급] 오전 타바타 루틴")
    private String title;

    @Schema(description = "수업 정원", example = "10")
    private int capacity;

    @Schema(description = "수업 소요 시간 (분)", example = "50")
    private String description;

    @Schema(description = "수업 소요 시간 (분)", example = "50")
    private int durationMinutes;

    @Schema(description = "난이도", example = "BEGINNER")
    private RecommendLevel recommendLevel;

    @Schema(description = "수업 종류", example = "GROUP_ROUTINE")
    ClassKind classKind;

    public static TemplateResponse create(Template template) {
        return TemplateResponse.builder()
                .templateId(template.getTemplateId())
                .title(template.getTitle())
                .description(template.getDescription())
                .capacity(template.getCapacity())
                .durationMinutes(template.getDurationMinutes())
                .recommendLevel(template.getRecommendLevel())
                .classKind(template.getClassKind())
                .build();
    }

}
