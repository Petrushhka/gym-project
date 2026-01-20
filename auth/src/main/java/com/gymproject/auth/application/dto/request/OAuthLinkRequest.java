package com.gymproject.auth.application.dto.request;

import com.gymproject.common.security.AuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "소셜 연동 요청 (Auth Code 전달)")
public class OAuthLinkRequest {

    @Schema(description = "소셜 제공자 타입", example = "GOOGLE")
    @NotNull(message = "Provider is required")
    private AuthProvider authProvider;

    @Schema(description = "소셜 로그인 후 발급받은 인가 코드 (Authorization Code)", example = "4/0AeaYSH...")
    @NotBlank(message = "Auth planeCode is required")
    private String authCode;

}
