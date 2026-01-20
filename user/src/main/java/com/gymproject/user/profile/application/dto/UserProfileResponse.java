package com.gymproject.user.profile.application.dto;

import com.gymproject.common.security.Roles;
import com.gymproject.user.profile.domain.vo.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "사용자 프로필 정보 응답 DTO")
public record UserProfileResponse(
        @Schema(description = "사용자 고유 식별자 (ID)", example = "1")
        Long id,
        @Schema(description = "사용자 이메일", example = "test@naver.com")
        String email,
        @Schema(description = "사용자 이름", example = "gildong Hong")
        String userName,
        @Schema(description = "사용자 휴대폰 번호 정보", example="04392872")
        PhoneNumber phoneNumber,
        @Schema(description = "사용자 권한", example = "MEMBER")
        Roles role
) {
}
