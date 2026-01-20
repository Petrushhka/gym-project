package com.gymproject.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "이메일 인증 요청")
public class SendVerificationCodeRequest {

    @Schema(description = "인증을 진행할 이메일", example = "test@naver.com")
    @NotBlank
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

}
