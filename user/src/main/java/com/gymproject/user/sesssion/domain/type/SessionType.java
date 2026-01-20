package com.gymproject.user.sesssion.domain.type;

public enum SessionType {
    FREE_TRIAL,
    PAID; // 유료세션, 무료 체험권
}
/*
    이게 booking 모듈에서
    TicketType과 같은 의미를 가지는 타입임.

    그러나 변화의 전파방지(Decoupling)을 위해 따로 의미가 같은 중복된 타입을 만든것임

    다음 예제 상황
    vip 전용 무료이용권을 추가하려고함, 무료지만 트레이너 승인 없이 바로 예약 확정이 되어야함.

    이런 경우에 TicketType과 SessionType은 의미가 서로 다름

    TicketType: 예약 로직의 분기 기준(즉시 확정 또는 대기)
    SessionType: 상품의 종류(무료냐 유료냐)
 */

/* 수정
    TicketType과는 다른 흐름 제어를함.
 */