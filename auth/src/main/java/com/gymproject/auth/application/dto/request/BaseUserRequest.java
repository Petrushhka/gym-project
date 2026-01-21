package com.gymproject.auth.application.dto.request;

import com.gymproject.common.event.domain.ProfileInfo;
import com.gymproject.common.security.SexType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "회원가입 시 사용자 공통 요청 정보")
public class BaseUserRequest {

    @Schema(description = "이름", example = "Gildong")
    @NotBlank(message = "First name is required")
    String firstName;

    @Schema(description = "성", example = "Hong")
    @NotBlank(message = "Last name is required")
    String lastName;

    @Schema(description = "이메일 (인증 완료된 이메일)", example = "test@naver.com")
    @NotBlank(message = "email is required")
    @Email(message = "이메일 형식이 맞지 않습니다.")
    String email;

    @Schema(description = "성별(M,F,X)", example = "M")
    @NotBlank(message = "sex is required")
    String gender; // 문자열로 받고 내부에서 변환

    @Schema(description = "휴대폰 번호 (국가코드 포함)", example = "+61412345678")
    @NotBlank(message = "phone number si required")
    @Pattern(regexp = "^(?:\\+61[-\\s]?4\\d{2}[-\\s]?\\d{3}[-\\s]?\\d{3}|04\\d{2}[-\\s]?\\d{3}[-\\s]?\\d{3})$",
            message = "Please enter a valid Australian mobile number (e.g. 0412 345 678 or +61 412 345 678).")
    String phoneNumber;

    @Schema(hidden = true) // 이 메서드를 스웨거 스키마에서 숨김(Getter때문에 스웨거 문서에 드러남)
    public ProfileInfo getProfileInfo() {
        return new ProfileInfo(
                this.getFirstName(),
                this.getLastName(),
                this.phoneNumber,
                SexType.valueOf(this.gender)
        );
    }

}
