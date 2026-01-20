package com.gymproject.auth.application.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gymproject.common.security.Roles;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record LoginResponse(
        @JsonIgnore // 프론트엔드(JSON)으로 갈 때는 안보이게됨.
        @Schema(hidden = true) // swagger에서 가림
        String refreshToken,

        @Schema(hidden = true)
        @JsonIgnore
        Long refreshTokenDuration,

        @Schema(description = "Access Token",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9...")
        String accessToken,

        @Schema(description = "사용자 이메일", example = "test@naver.com")
        String email,

        @Schema(description = "사용자 고유 ID (PK)", example = "1")
        Long userId,

        @Schema(description = "사용자 실명", example = "홍길동")
        String userName,

        @Schema(description = "사용자 권한", example = "TRAINER")
        Roles role
) {
}
