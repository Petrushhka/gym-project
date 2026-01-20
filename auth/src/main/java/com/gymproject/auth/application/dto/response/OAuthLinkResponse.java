package com.gymproject.auth.application.dto.response;

import com.gymproject.common.security.AuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "소셜 연동 응답", example = "기존 사용자 소셜 연동 응답")
public record OAuthLinkResponse(
        @Schema(description = "처리 결과 메시지", example = "GOOGLE 계정과 성공적으로 연동되었습니다.")
        String message,

        @Schema(description = "연동된 제공자 타입", example = "GOOGLE")
        AuthProvider provider) {
}
