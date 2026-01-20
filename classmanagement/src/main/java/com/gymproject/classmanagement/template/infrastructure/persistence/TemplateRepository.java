package com.gymproject.classmanagement.template.infrastructure.persistence;

import com.gymproject.classmanagement.template.domain.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    Optional<Template> findByTemplateId(Long templateId);
}
