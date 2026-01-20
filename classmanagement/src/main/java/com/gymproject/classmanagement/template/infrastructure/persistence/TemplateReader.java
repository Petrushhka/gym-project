package com.gymproject.classmanagement.template.infrastructure.persistence;

import com.gymproject.classmanagement.template.domain.entity.Template;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemplateReader {
    private final TemplateRepository templateRepository;

    public Template getTemplate(Long templateId) {
        return templateRepository.findById(templateId).orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 템플릿입니다. ID: " + templateId)
        );
    }
}
