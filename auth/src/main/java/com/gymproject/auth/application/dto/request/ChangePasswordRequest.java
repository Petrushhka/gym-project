package com.gymproject.auth.application.dto.request;

import com.gymproject.auth.exception.IdentityErrorCode;
import com.gymproject.auth.exception.IdentityException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "비밀번호 변경 요청(로그인 상태)")
public class ChangePasswordRequest {

    // 1. 현재 비밀번호 (보안 확인용)
    @Schema(description = "현재 비밀번호", example = "Password123!")
    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    private String currentPassword;

    // 2. 바꿀 비밀번호
    @Schema(description = "새로운 비밀번호", example = "NewP@ssword1!")
    @NotBlank
    @Pattern(regexp = SignUpRequest.PASSWORD_REGEX, message = SignUpRequest.PASSWORD_MSG)
    private String newPassword;

    @Schema(description = "새로운 비밀번호 확인", example = "NewP@ssword1!")
    @NotBlank
    private String newPasswordConfirm;

    public void normalize(){
        this.currentPassword = currentPassword.trim();
        this.newPassword = newPassword.trim();
    }

    public void validatePasswordMismatch(){
        if(this.newPassword == null || !this.newPassword.equals(this.newPasswordConfirm)){
            throw new IdentityException(IdentityErrorCode.PASSWORD_MISMATCH);
        }
    }

    // (선택) 현재 비밀번호와 새 비밀번호가 같은지 체크하는 로직도 추가 가능
    public void validateNewPasswordSameAsCurrent() {
        if (this.currentPassword.equals(this.newPassword)) {
            throw new IdentityException(IdentityErrorCode.PASSWORD_SAME_AS_CURRENT);
        }
    }

}
