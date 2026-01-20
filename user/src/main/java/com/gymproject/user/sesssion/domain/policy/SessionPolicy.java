package com.gymproject.user.sesssion.domain.policy;

import com.gymproject.user.sesssion.exception.UserSessionErrorCode;
import com.gymproject.user.sesssion.exception.UserSessionsException;

import java.time.OffsetDateTime;

public final class SessionPolicy {

    // -- 정책 상수
    // 무료세션권은 한달이내 사용
    private static final long FREE_TRIAL_PERIOD_DAYS = 30L;

    // 1. 생성 시 만료일 계산(무료 티켓만, 유료 세션권은 결제된 내용을 바탕으로 만들어짐)
    public static OffsetDateTime calculateFreeTrialExpiredAt(OffsetDateTime now) {
        return now.plusDays(FREE_TRIAL_PERIOD_DAYS);
    }

    public static OffsetDateTime calculatePaidExpiredAt(OffsetDateTime now, int duration) {
        return now.plusDays(duration);
    }

    // 2. 사용 가능 여부 검증
    public static void validateExpiry(OffsetDateTime expireAt, OffsetDateTime now) {
        if(expireAt != null && expireAt.isBefore(now)) {
            throw new UserSessionsException(UserSessionErrorCode.EXPIRED, expireAt.toString());
        }
    }

    // 3. 복구 정책
    public static void validateRestoreAmount(int usedSession){
        if(usedSession <= 0){
            throw new UserSessionsException(UserSessionErrorCode.RESTORE_NOT_ALLOWED);
        }
    }

    // 4. 환불 정책(횟수 기반), 세션권을 하나라도 사용하면 환불 불가
    public static void validateForRefund(int usedSession, OffsetDateTime expireAt){
        if(usedSession > 0){
            throw new UserSessionsException(UserSessionErrorCode.ALREADY_USED);
        }
    }

    // 세션권 환불 비율 계산
    public static double calculateRefundRate(int usedSession){
        // 사용 횟수가 0일 때는 100% 환불, 나머지는 0%
        return usedSession == 0 ? 1.0 : 0.0;
    }
}
