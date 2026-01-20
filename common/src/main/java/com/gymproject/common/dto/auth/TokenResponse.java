package com.gymproject.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    Long refreshTokenDuration; // 리프레시 토큰 수명(ttl)
}
