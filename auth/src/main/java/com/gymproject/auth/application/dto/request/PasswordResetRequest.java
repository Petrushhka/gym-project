package com.gymproject.auth.application.dto.request;

import com.gymproject.auth.exception.IdentityErrorCode;
import com.gymproject.auth.exception.IdentityException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "비밀번호 재설정 요청 (비로그인용)")
public class PasswordResetRequest {

    @Schema(description = "코드 검증 단계에서 발급받은 비밀번호 재설정 토큰",
            example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank
    private String resetToken;

    @Schema(description = "새로운 비밀번호", example = "NewP@ssword1!")
    @NotBlank
    @Pattern(regexp = SignUpRequest.PASSWORD_REGEX, message = SignUpRequest.PASSWORD_MSG)
    private String password;

    @Schema(description = "새로운 비밀번호 확인", example = "NewP@ssword1!")
    @NotBlank
    private String passwordConfirm;

    public void normalize(){
        this.password = password.trim();
        this.passwordConfirm = passwordConfirm.trim();
    }

    public void validatePasswordMismatch(){
        if(this.password == null || !this.password.equals(this.passwordConfirm)){
            throw new IdentityException(IdentityErrorCode.PASSWORD_MISMATCH);
        }
    }
}
