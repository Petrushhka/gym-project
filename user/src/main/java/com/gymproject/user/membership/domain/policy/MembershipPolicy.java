package com.gymproject.user.membership.domain.policy;

import com.gymproject.user.membership.exception.UserMembershipErrorCode;
import com.gymproject.user.membership.exception.UserMembershipException;

import java.time.Duration;
import java.time.OffsetDateTime;

public final class MembershipPolicy {
    // --- 정책 상수들 ---
    public static final int MAX_SUSPEND_DAYS = 90;
    public static final int EXTENSION_PERIOD_MONTHS = 3;
    public static final int PARTIAL_REFUND_LIMIT_DAYS = 14;


    // 1. 생성 검증 (멤버십 시작일이 과거이면 안됨)
    public static void validateStartDate(OffsetDateTime startDate, OffsetDateTime now) {
        if(startDate.isBefore(now)){
            throw new UserMembershipException(UserMembershipErrorCode.INVALID_DATE);
        }
    }

    // 2. 연장 가능 여부 판단(멤버십 종료기간 3달 이내로 연장가능)
    public static void validateExtendablePeriod(OffsetDateTime expiredAt, OffsetDateTime now) {
        if(expiredAt.isBefore(now.minusMonths(EXTENSION_PERIOD_MONTHS))) {
            throw new UserMembershipException(UserMembershipErrorCode.EXTENSION_PERIOD_EXCEED);
        }
    }

    // 3. 연장 시작일 계산 규칙
    // 멤버십 만료일이 안 지났으면 만료일 부터, 지났으면 지금부터 기간연장
    public static OffsetDateTime calculateExtensionBaseDate(OffsetDateTime expiredAt, OffsetDateTime now) {
        return expiredAt.isBefore(now) ? now : expiredAt;
    }

    // 4. 정지 가능 기간 검증 로직(최대 90일 이내로만 정지가능)
    public static void validateSuspendPeriod(OffsetDateTime startAt, OffsetDateTime endAt) {
        if(startAt.isAfter(endAt)) {
            throw new UserMembershipException(UserMembershipErrorCode.INVALID_SUSPEND_PERIOD);}

        long requestDays = Duration.between(startAt, endAt).toDays();
        if(requestDays > MAX_SUSPEND_DAYS) {
            throw new UserMembershipException(UserMembershipErrorCode.EXCEED_MAX_SUSPEND_DAYS);}
        if(requestDays <= 0) {
            throw new UserMembershipException(UserMembershipErrorCode.MIN_SUSPEND_DAYS_REQUIRED);}

    }
    // 정지 시작일이 과거이면 안됨.
    public static void validateSuspendStartTime(OffsetDateTime startAt, OffsetDateTime now) {
        if (startAt.isBefore(now)) {
            throw new UserMembershipException(UserMembershipErrorCode.CANNOT_SUSPEND_PAST);
        }
    }

    // 5. 전액 환불(Cancel) 정책
    public static void validateCancelPeriod(OffsetDateTime startAt, OffsetDateTime now) {
        // 14일이 지났는지
        checkExceed(startAt, now);

        // 부분환불 대상자인지(시작 후 14일 이전까지)
        if(now.isAfter(startAt) && now.isBefore(startAt.plusDays(PARTIAL_REFUND_LIMIT_DAYS))) {
            throw new UserMembershipException(UserMembershipErrorCode.REFUND_ROLLBACK);
        }
    }

    // 6. 부분 환불(Rollback) 정책
    public static void validateRollbackPeriod(OffsetDateTime startedAt, OffsetDateTime refundDate){
        // 시작 한적 있는지
        if(refundDate.isBefore(startedAt)) {
            throw new UserMembershipException(UserMembershipErrorCode.NOT_STARTED_YET);
        }

        checkExceed(startedAt, refundDate);
    }

    // 7. 환불 비율 계산 공식
    public static double calculateRefundRate(OffsetDateTime startedAt, OffsetDateTime now) {
        // 시작 전이면 100%
        if (now.isBefore(startedAt)) {
            return 1.0;
        }

        // 14일 이내라면 일할 계산
        if (now.isBefore(startedAt.plusDays(PARTIAL_REFUND_LIMIT_DAYS))) {
            // toDays()는 버림이라서 사용하면 안됨.(1시간 뒤에 취소하더라도 전액환불됨)
            long usedSeconds = Duration.between(startedAt, now).getSeconds();

            // 올림 계산: (사용한 초 + 하루초  -1) / 86400 올림처리
            long usedDays = (usedSeconds + 86400 - 1) / 86400;

            // 만약에 0초라면 환불
            if(usedDays == 0) usedDays = 1;

            return (double) (PARTIAL_REFUND_LIMIT_DAYS - usedDays) / PARTIAL_REFUND_LIMIT_DAYS;
        }

        // 그 외는 0%
        return 0.0;
    }

    // 14일이 지났으면 전액/부분 환불은 못함
    private static void checkExceed(OffsetDateTime startedAt, OffsetDateTime refundDate) {
        // 14일이 지났는지
        if(refundDate.isAfter(startedAt.plusDays(PARTIAL_REFUND_LIMIT_DAYS))) {
            throw new UserMembershipException(UserMembershipErrorCode.REFUND_PERIOD_EXCEEDED);
        }
    }

}
