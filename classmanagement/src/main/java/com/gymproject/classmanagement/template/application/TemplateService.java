package com.gymproject.classmanagement.template.application;

import com.gymproject.classmanagement.template.application.dto.TemplateRequest;
import com.gymproject.classmanagement.template.application.dto.TemplateResponse;
import com.gymproject.classmanagement.template.domain.entity.Template;
import com.gymproject.classmanagement.template.infrastructure.persistence.TemplateReader;
import com.gymproject.classmanagement.template.infrastructure.persistence.TemplateRepository;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.port.auth.IdentityQueryPort;
import com.gymproject.common.util.GymDateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateReader templateReader;
    private final IdentityQueryPort identityQueryPort;

    // 1. 템플릿 생성
    @Transactional
    public TemplateResponse createTemplate(TemplateRequest request, UserAuthInfo userAuthInfo) {
        identityQueryPort.validateTrainer(userAuthInfo.getUserId());

        Template template = Template.create(
                request.getTitle(), request.getDescription(),
                request.getCapacity(), request.getDurationMinutes(),
                request.getRecommendLevel(), request.getClassKind()
        );

        return TemplateResponse.create(templateRepository.save(template));
    }

    // 2. 템플릿 삭제(소프트)
    public TemplateResponse deleteTemplate(Long templateId, UserAuthInfo userAuthInfo) {
        identityQueryPort.validateTrainer(userAuthInfo.getUserId());
        Template template = templateReader.getTemplate(templateId);

        template.softDelete(GymDateUtil.now());
        templateRepository.delete(template);

        return TemplateResponse.create(template);
    }


}
