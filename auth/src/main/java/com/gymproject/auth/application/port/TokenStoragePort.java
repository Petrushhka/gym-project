package com.gymproject.auth.application.port;

public interface TokenStoragePort {
    // 1. 저장된 토큰과 요청된 토큰이 일치하는지 검증
    void verifyTokenMatch(Long userId, String requestToken);

    // 2. 리프레시 토큰 저장 및 갱신 (TTL 포함)
    void updateRefreshToken(Long userId, String newToken, long ttlMillis);

    // 3. 토큰 삭제(로그아웃/탈퇴 시)
    void deleteRefreshToken(Long userId);
}

/*
    [중요] 매우

    JPA는 쓰면서 왜 Redis만 유난 떨면서 포트,어댑터로 구현하냐?

    블로에 글 올려놓음.
 */