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
@Schema(description = "일반 회원가입 요청 데이터")
public class SignUpRequest extends BaseUserRequest {

    // 정규식 상수화 (다른 클래스에서도 쓰기 위함)
    public static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,20}$";
    public static final String PASSWORD_MSG = "비밀번호는 영문 대소문자, 숫자, 특수문자를 포함한 8~20자여야 합니다.";

    @Schema(description = "비밀번호 (영문 대소문자, 숫자, 특수문자 포함 8~20자)",
            example = "P@ssword123!")
    @NotBlank
    @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MSG)
    private String password;

    @Schema(description = "비밀번호 확인", example = "P@ssword123!")
    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String passwordConfirm;

    // 비빌번호 공백 없애기
    public void normalize() {
        if (this.password != null) this.password = this.password.trim();
        if (this.passwordConfirm != null) this.passwordConfirm = this.passwordConfirm.trim();
    }

    public void validatePasswordMismatch() {
        if (this.password == null || !this.password.equals(this.passwordConfirm)) {
            throw new IdentityException(IdentityErrorCode.PASSWORD_MISMATCH);
        }
    }
}



