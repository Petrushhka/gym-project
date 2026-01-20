package com.gymproject.auth.infrastructure.adapter;

import com.gymproject.auth.application.port.TokenStoragePort;
import com.gymproject.auth.exception.IdentityErrorCode;
import com.gymproject.auth.exception.IdentityException;
import com.gymproject.support.redis.RedisTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisTokenAdapter implements TokenStoragePort {

    private final RedisTool redisTool;
    private static final String RT_PREFIX = "RT:";

    // 1. 토큰 검증
    @Override
    public void verifyTokenMatch(Long userId, String requestToken) {
        String key = RT_PREFIX + userId;
        String savedToken = redisTool.getValues(key);

        // 토큰이 없거나, 클라이언트가 보낸 것과 다르면 탈취 의심
        if (savedToken == null || !savedToken.equals(requestToken)) {
            throw new IdentityException(IdentityErrorCode.TOKEN_STOLEN);
        }
    }

    // 2. 토큰 갱신
    @Override
    public void updateRefreshToken(Long userId, String newToken, long ttlMillis) {
        String key = RT_PREFIX + userId;
        redisTool.setValues(key, newToken, Duration.ofMillis(ttlMillis));
    }

    // 3. 로그아웃 또는 탈퇴 시 토큰 삭제
    @Override
    public void deleteRefreshToken(Long userId) {
        redisTool.deleteValues(RT_PREFIX + userId);
    }
}
