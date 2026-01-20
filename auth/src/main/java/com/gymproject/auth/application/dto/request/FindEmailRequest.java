package com.gymproject.auth.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "이메일 찾기 요청")
public class FindEmailRequest {
    @Schema(description = "가입 시 등록한 휴대폰 번호", example = "+61412345678")
    @NotBlank(message = "전화번호는 필수 입니다.")
    private String phoneNumber;
}
