package com.gymproject.user.membership.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserMembershipErrorCode {

    // 404: Not Found
    NOT_FOUND("해당 멤버십을 찾을 수 없습니다.", 404, "MEMBERSHIP_404"),

    // 400: Invalid Request (Business Logic)
    INVALID_STATUS("현재 멤버십 상태에서는 해당 작업을 수행할 수 없습니다.", 400, "MEMBERSHIP_INVALID_STATUS"),
    ALREADY_PROCESSED("이미 만료되거나 취소된 멤버십입니다.", 400, "MEMBERSHIP_ALREADY_PROCESSED"),

    // 정지 관련 (Suspend)
    NOT_ACTIVE("활성(ACTIVE) 상태의 멤버십만 일시 정지할 수 있습니다.", 400, "MEMBERSHIP_NOT_ACTIVE"),
    NOT_SUSPENDED("정지(SUSPENDED) 상태가 아닙니다.", 400, "MEMBERSHIP_NOT_SUSPENDED"),
    CANNOT_SUSPEND_PAST("과거 시점으로 정지를 예약할 수 없습니다.", 400, "MEMBERSHIP_SUSPEND_PAST"),
    INVALID_SUSPEND_PERIOD("정지 종료일은 시작일 이후여야 합니다.", 400, "MEMBERSHIP_SUSPEND_PERIOD_INVALID"),
    EXCEED_MAX_SUSPEND_DAYS("최대 정지 가능 기간(90일)을 초과했습니다.", 400, "MEMBERSHIP_SUSPEND_LIMIT_EXCEEDED"),
    MIN_SUSPEND_DAYS_REQUIRED("정지 기간은 최소 1일 이상이어야 합니다.", 400, "MEMBERSHIP_SUSPEND_MIN_REQUIRED"),

    // 만료/날짜 관련 (Expire)
    NOT_YET_EXPIRED("아직 만료 예정 시각에 도달하지 않았습니다.", 400, "MEMBERSHIP_NOT_YET_EXPIRED"),
    INVALID_DATE("과거 날짜로 멤버십을 생성할 수 없습니다.", 400, "MEMBERSHIP_INVALID_DATE"),
    EXTENSION_PERIOD_EXCEED("만료 후 연장 가능 기간(3개월)이 지났습니다. 신규가입을 진행해주세요.", 400,"MEMBERSHIP_EXTENSION_PERIOD_EXCEEDED"),

    // 환불/취소 관련 (Cancel/Refund)
    REFUND_PERIOD_EXCEEDED("시작일로부터 14일이 지난 멤버십은 환불 불가합니다.", 400, "REFUND_PERIOD_EXCEEDED"),
    REFUND_ROLLBACK("이용기간이 지난 후 14일 이전엔 부분환불만 가능합니다.", 400, "MEMBERSHIP_ROLLBACK"),
    NOT_STARTED_YET("아직 이용 시작 전입니다. 전액 환불/취소를 이용해주세요.", 400, "MEMBERSHIP_NOT_STARTED_YET"),
    // 상품 관련
    INVALID_PLAN_TYPE("작업을 수행할 수 없는 플랜입니다.", 400, "INVALID_PLAN_TYPE"),
    UNSUPPORTED_CONTRACT_VERSION("지원하지 않는 계약서 버전입니다.", 400 ,"UNSUPPORTED_CONTRACT_VERSION"),

    // 연속 수업보다 종료일이 짧은 멤버십
    INSUFFICIENT_MEMBERSHIP_PERIOD("멤버십 종료 기간이 요청하신 일자보다 짧습니다.", 400, "INSUFFICIENT_MEMBERSHIP_PERIOD"),
    ALREADY_EXIST("기존에 존재하는 멤버십 플랜입니다.", 400 , "MEMBERSHIP_ALREADY_EXIST" ),;

    private final String message;
    private final int statusCode;
    private final String errorCode;
}
