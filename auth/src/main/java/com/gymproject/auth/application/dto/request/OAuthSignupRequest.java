package com.gymproject.auth.application.dto.request;

import com.gymproject.common.security.AuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "소셜 회원가입 요청 후 추가 정보 입력")
@NoArgsConstructor
public class OAuthSignupRequest extends BaseUserRequest {

        //@NotBlank < Enum에서 사용 못함 NotBlank는 공란까지 검사하는거라서
        @Schema(description = "소셜 제공자", example = "GOOGLE")
        @NotNull(message = "Provider is required")
        private AuthProvider authProvider;

        @Schema(description = "소셜 서비스의 고유 사용자 ID (sub)", example = "1029384756123456")
        @NotBlank(message = "Provider USer ID is required")
        private String providerUserId;
}


