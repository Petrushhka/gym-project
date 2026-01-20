package com.gymproject.auth.infrastructure.email;

import com.gymproject.auth.exception.IdentityErrorCode;
import com.gymproject.auth.exception.IdentityException;
import com.gymproject.support.email.EmailTool;
import com.gymproject.support.redis.RedisTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Random;
import java.util.UUID;

/**[중요]
 * RESET_TOKEN
 * 6자리 숫자는 브루트포스 공격 같은걸 받으면 막을 수 없음.
 * 따라서 토큰을 따로 발급하여 좀 더 보안을 늘리는 것임.
 * 보안토큰 자체는 여유롭게 시간을 두어도 좋음
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationManager {

    private final RedisTool redisTool;
    private final EmailTool emailTool;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom(); // 난수생성기 하나로만 사용
    private static final String AUTH_CODE_PREFIX = "AuthCode:";
    private static final String RESET_TOKEN_PREFIX = "ResetToken:";
    private static final String VERIFIED_EMAIL_PREFIX = "VerifiedEmail:";
    // 상수: 리셋 토큰 유효시간( 10분)
    private static final long RESET_TOKEN_VALIDITY_MS = 10 * 60 * 1000L;

    @Value("${spring.mail.auth-code-expiration-millis}")
    private long authCodeExpirationMillis;


    // 1. 이메일 발송
    public void sendCodeToEmail(String email) {
        String title = "유저 이메일 인증 번호";
        String authCode = createCode();

        // Redis 저장 (Key: AuthCode:이메일, Value: 123456)
        redisTool.setValues(
                AUTH_CODE_PREFIX + email,
                authCode,
                Duration.ofMillis(authCodeExpirationMillis)
        );

        // 실제 메일 발송
        emailTool.sendMail(email, title, authCode);
    }

    // 2. 인증 코드 검증 (6자리 숫자 비교)
    public boolean isValidCode(String email, String authCode) {
        // 이메일 발송 시 저장했던 키 패턴
        String storedCode = redisTool.getValues(AUTH_CODE_PREFIX + email);

        //코드가 존재하고, 입력값과 일치하는지 확인
        return storedCode != null && storedCode.equals(authCode);
    }

    // 회원가입용 이메일 인증 확인 및 처리
    public void verifySignupEmail(String email, String authCode) {
        if (!isValidCode(email, authCode)) {
            throw new IdentityException(IdentityErrorCode.CODE_UNMATCHED);
        }

        // 인증 성공 시 "VerifiedEmail" 마킹 (가입할 때 확인용)
        redisTool.setValues(
                VERIFIED_EMAIL_PREFIX + email,
                "true",
                Duration.ofMinutes(30) // 30분 내에 가입해야 함
        );

        // 사용한 인증 코드는 삭제
        redisTool.deleteValues(AUTH_CODE_PREFIX + email);
    }

    // 3. 리셋 토큰 생성 및 저장
    public String createResetToken(String email) {
        // 예측 불가능한 랜덤 문자열 생성
        String resetToken = UUID.randomUUID().toString();

        // Redis에 저장(Key: 토큰, Value: 이메일 ) -> 토큰만 알면 이메일 알 수 있음
        redisTool.setValues(
                RESET_TOKEN_PREFIX + resetToken,
                email,
                Duration.ofMillis(RESET_TOKEN_VALIDITY_MS));

        // 리셋 토큰 발급되면 인증 코드는 삭제
        redisTool.deleteValues(AUTH_CODE_PREFIX + email);
        return resetToken;
    }

    // 4. 리셋 토큰 검증 및 이메일 추출
    public String verifyResetToken(String resetToken) {
        String email = redisTool.getValues(RESET_TOKEN_PREFIX + resetToken);
        if(email == null){
            throw new IdentityException(IdentityErrorCode.INVALID_TOKEN);
        }
        return email;
    }

    // 5. 리셋 토큰 삭제(사용 후)
    public void deleteResetToken(String resetToken) {
        redisTool.deleteValues(RESET_TOKEN_PREFIX + resetToken);
    }


    // 6. 이메일 인증 완료 여부 확인(가입 시)
    public boolean checkExistsValue(String email) {
        String verified =  redisTool.getValues(VERIFIED_EMAIL_PREFIX + email);
        // 이메일 인증 됨 (T), 안됨(F)
        return verified != null && verified.equals("true");
    }


    // 7. 가입 완료 후 인증 마킹 삭제 (재가입방지)
    public void invalidateVerifiedEmail(String email) {
        redisTool.deleteValues(VERIFIED_EMAIL_PREFIX + email);
    }

    // 난수 생성
    private static String createCode() {
        try {
            Random random = SECURE_RANDOM.getInstanceStrong();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                builder.append(random.nextInt(10));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            // 서버 환경 문제이므로 RuntimeException 처리
            throw new RuntimeException("SecureRandom 알고리즘을 찾을 수 없습니다.", e);
        }
    }

}
