package com.gymproject.classmanagement.api;

import com.gymproject.classmanagement.template.application.TemplateService;
import com.gymproject.classmanagement.template.application.dto.TemplateRequest;
import com.gymproject.classmanagement.template.application.dto.TemplateResponse;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.exception.CommonResDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1. 수업 템플릿 관리", description = "수업 템플릿 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateService templateService;

    @Operation(
            summary = "1. 수업 템플릿 생성", description = """
            반복되는 수업의 구성(수업명, 시간 정보, 정원 등)을 템플릿으로 저장합니다.
            
            1. 매번 수업을 개별 등록하지 않고, 저장된 템플릿을 불러와 특정 기간의 스케줄을 대량 생성할 때 사용합니다.
            2. 현재 로그인한 트레이너 본인의 템플릿으로 저장됩니다.
            """
    )
    @PostMapping
    public ResponseEntity<CommonResDto<TemplateResponse>> createTemplate(
            @RequestBody TemplateRequest dto,
            @AuthenticationPrincipal UserAuthInfo userAuthInfo) {

        TemplateResponse response = templateService.createTemplate(dto, userAuthInfo);

        return ResponseEntity.ok(CommonResDto.success(
                HttpStatus.CREATED.value(),
                "수업 템플릿이 등록되었습니다.",
                response
        ));
    }

    @Operation(
            summary = "2. 수업 템플릿 삭제",
            description = "등록된 수업 템플릿을 삭제합니다. (템플릿으로 생성된 실제 수업 스케줄에는 영향을 주지 않습니다.)"
    )
    @DeleteMapping("/{templateId}")
    public ResponseEntity<CommonResDto<TemplateResponse>> deleteTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal UserAuthInfo userAuthInfo) {

        TemplateResponse response = templateService.deleteTemplate(templateId, userAuthInfo);

        return ResponseEntity.ok(CommonResDto.success(
                HttpStatus.OK.value(), "수업 템플릿이 삭제되었습니다.", response
        ));
    }


}
