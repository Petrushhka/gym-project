package com.gymproject.classmanagement.template.domain.entity;

import com.gymproject.classmanagement.template.domain.policy.TemplatePolicy;
import com.gymproject.classmanagement.template.domain.type.ClassKind;
import com.gymproject.classmanagement.template.domain.type.RecommendLevel;
import com.gymproject.classmanagement.template.exception.TemplateErrorCode;
import com.gymproject.classmanagement.template.exception.TemplateException;
import com.gymproject.common.util.GymDateUtil;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "CLASS_TEMPLATE_TB")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommend_level")
    private RecommendLevel recommendLevel;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "class_kind", nullable = false)
    private ClassKind classKind;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // 호주시간으로 저장
    @PrePersist
    public void onPrePersist() {
        OffsetDateTime now = GymDateUtil.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // 호주시간으로 저장
    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = GymDateUtil.now();
    }

    @Builder(access = AccessLevel.PRIVATE)
    private Template(String title, String description,
                     int capacity, int durationMinutes, RecommendLevel recommendLevel,
                     ClassKind classKind) {
        this.title = title;
        this.description = description;
        this.capacity = capacity;
        this.durationMinutes = durationMinutes;
        this.recommendLevel = recommendLevel;
        this.classKind = classKind;
    }


    // 1. 템플릿 생성
    public static Template create(String title, String description,
                                  int capacity, int durationMinutes,
                                  RecommendLevel level, ClassKind kind) {

        validateTemplate(title, capacity, durationMinutes, kind);

        return Template.builder()
                .title(title)
                .description(description)
                .capacity(capacity)
                .durationMinutes(durationMinutes)
                .recommendLevel(level)
                .classKind(kind)
                .build();
    }

    // 2. 템플릿 수정
    public void update(String title, String description, int capacity,
                       int durationMinutes, RecommendLevel level, ClassKind kind) {
            if(isDeleted()) {
                throw new TemplateException(TemplateErrorCode.ALREADY_DELETED);
            }
            // 수정 시에도 동일한 정책 검증 수행
            validateTemplate(title, capacity, durationMinutes, kind);

            this.title = title;
            this.description = description;
            this.capacity = capacity;
            this.durationMinutes = durationMinutes;
            this.recommendLevel = level;
            this.classKind = kind;
    }

    // 3. 템플릿 삭제
    public void softDelete(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    //  === 헬퍼
    public boolean isDeleted() {
        return deletedAt != null;
    }

    // == 검증
    private static void validateTemplate(String title, int capacity, int durationMinutes, ClassKind kind) {
        if(title == null || title.isBlank()) throw new TemplateException(TemplateErrorCode.INVALID_TITLE);

        TemplatePolicy.validate(capacity,durationMinutes,kind);
    }
    /* 엔티티는 DTO(뒙계층)에 대한 정보를 몰라야함. 순수성을 유지하기위해, 그러나 DTO는 엔티티를 알아도됨(변환목적으로)
    public static Template create(TemplateRequest dto) {
        return Template.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .capacity(dto.getCapacity())
                .durationMinutes(dto.getDurationMinutes())
                .recommendLevel(dto.getRecommendLevel())
                .classKind(dto.getClassKind())
                .build();
    }
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Template)) return false;
        Template other = (Template) o;

        if (this.templateId == null || other.templateId == null) {
            return false;  // 아직 영속화되지 않은 비교는 false 처리
        }

        return this.templateId.equals(other.templateId);
    }

    @Override
    public int hashCode() {
        return (templateId != null) ? templateId.hashCode() : 0;
    }
}
