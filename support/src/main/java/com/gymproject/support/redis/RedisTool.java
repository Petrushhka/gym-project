package com.gymproject.support.redis;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Component
@Transactional
@RequiredArgsConstructor
public class RedisTool {

    private static final Logger log = LoggerFactory.getLogger(RedisTool.class);

    private final RedisTemplate<String, Object> redisTemplate;

//    private static final String RT_PREFIX = "RT:"; // Key 값의 접두사

    // 1. 저장 (만료시간 없이 단순 캐싱)
    public void setValues(String key, String data) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, data);
    }

    // 2. 저장 (만료시간 있음)
    public void setValues(String key, String data, Duration duration) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key, data, duration);
    }

    // 3. 조회
    public String getValues(String key) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        if (values.get(key) == null) {
            return "false";
        }
        return (String) values.get(key);
    }

    // 4. 삭제
    public void deleteValues(String key) {
        Boolean result = redisTemplate.delete(key);
        if (Boolean.FALSE.equals(result)) {
            log.warn("레디스 삭제 실패 혹은 키 없음: {}", key);
        }
    }

    // 5. 확인
    // 존재 확인
    public boolean checkExistsValue(String value) {
        return !value.equals("false");
    }
}

/* 아래는 모듈 분리 전 토큰과 관련된 내용


    // 토큰 검증 및 비교(Rotation 체크)
    public void verifyTokenMatch(Long userId, String requestToken) {
        String key = RT_PREFIX + userId;
        String savedToken = (String)redisTemplate.opsForValue().get(key);

        // 토큰이 없거나, 클라이언트가 보낸 것과 다르면 탈취 의심
        if (savedToken == null || !savedToken.equals(requestToken)) {
            throw new IdentityException(IdentityErrorCode.TOKEN_STOLEN);
        }
    }

     새로운 토큰으로 갱신
    public void updateRefreshToken(Long userId, String newToken, long ttlMillis) {
        String key = RT_PREFIX + userId;
        redisTemplate.opsForValue().set(key, newToken, Duration.ofMillis(ttlMillis));
    }

    // 로그아웃 또는 탈퇴 시 토큰 삭제
    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(RT_PREFIX + userId);
    }


        // values.get(key)하고 delete를 보내는것은 레디스에게 요청을 두번보내야함.
        // 따라서 바로 delete

        /*
         ValueOperations<String, Object> values = redisTemplate.opsForValue();
        if(values.get(key) == null){
            throw new IllegalArgumentException("이메일 값이 없음");
        }
        redisTemplate.delete(key);

         */
