package com.gymproject.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "이메일 인증 코드 검증 요청")
public class VerifyCodeRequest {
    @Schema(description = "인증할 이메일", example = "test@naver.com")
    @NotBlank
    @Email
    private String email;

    @Schema(description = "전송된 인증 코드 6자리", example = "345925")
    @NotBlank
    private String code;
}
