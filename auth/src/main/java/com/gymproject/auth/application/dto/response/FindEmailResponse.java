package com.gymproject.auth.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 찾기 응답")
public record FindEmailResponse(
        @Schema(description = "마스킹된 이메일 주소", example = "te***@naver.com")
        String maskedEmail) {
}
