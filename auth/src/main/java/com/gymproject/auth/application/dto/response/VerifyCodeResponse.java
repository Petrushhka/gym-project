package com.gymproject.auth.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record VerifyCodeResponse(
        @Schema(description = "인증된 이메일", example = "test@naver.com")
        boolean verified, // 성공 여부

        @Schema(description = "비밀번호 재설정 권한 토큰 ", example = "550e8400-e29b-41d4-a716-446655440000")
        String resetToken) // 성공했을 때만 발급되는 토큰 (실패하면 null)
{
    // 실패했을 때 빠르게 만들기 위한 팩토리 메서드 (선택 사항)
    public static VerifyCodeResponse fail() {
        return new VerifyCodeResponse(false, null);
    }

    // 성공했을 때
    public static VerifyCodeResponse success(String token) {
        return new VerifyCodeResponse(true, token);
    }
}
