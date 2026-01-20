package com.gymproject.booking.booking.domain.type;

public enum BookingStatus {
    PENDING, CONFIRMED, ATTENDED, NOSHOW, CANCELLED, REJECTED
}
/*
    pending : 대기, 요청(무료회원인 사람들이 신청할 때)
    confirmed: 확정(무료회원만 트레이너 수락 필수, 세션 선차감 방식이므로, 트레이너 승인 불필요. 즉시 확정.)
    attended : 출석
    noshow: 노쇼족
    cancelled: 취소(환불해야함)
    rejected: 수업 거절, 취소와는 다름.(1:1 수업 거부임)
 */

/** 회원(GUEST)이 1:1 체험수업 신청 -

 1. 회원(guest)이 체험용 세션(FREE)로 1:1수업 신청
 2. 예약생성, PENDING 상태임
 3. 트레이너가 확인 후 CONFIRMED(예약 확정)

 또는
    트레이너 REJECTED(반려)

 * 수업신청은 현재시간으로 부터 최소 3시간전

 */

/** 회원(MEMBER) 1:1 수업 신청
 1. 회원(MEMBER)가 유료 세션(PAID)으로 1:1 수업 신청
 2, 예약 생성, 바로 CONFIRMED

 * 수업신천은 현재시간으로부터 최소 1시간 전
 */

/** 출석 방법 (ATTENDED vs NOSHOW)
 1. GPS 인증
 2. 이용권 1회 차감과 동시에 (ATTENDED)

 또는

    NOSHOW로 자동 처리
 */

/** 수업취소(Cancel)
 취소 요청자가 트레이너 / 회원일 수 있음

 1. 트레이너일 경우 환불

 2. 회원일 경우
  1) 24 시간 이전: 무조건 무료 취소(세션 복구)
  2) 24~1시간 전: 취소 가능 이용권 차감(세션 복구x)
  3) 1시간 이내: 취소 불가(전화)
  * 실수로 예약 생성일 수 있으니 10분 이내 예약 취소 가능

 */