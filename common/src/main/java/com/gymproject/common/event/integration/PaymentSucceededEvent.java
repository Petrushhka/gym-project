package com.gymproject.common.event.integration;

public record PaymentSucceededEvent (
        Long userId, // 누가 결제했는지
        Long paymentId, // 결제 아이디가 뭔지
        Long amount, // 얼마 계산하는지
        String category, // 결제하려는 상품이 뭔지(회원권, PT세션)
        String planeCode, // 상품 내용이 무엇인지 (1개월 이용권, 10회 PT권 등)
        String contractJson, // 결제 내용 (멤버십 시작일 등 Json 형태로옴)
        String paymentKey // PG사에서 전달하는 고유번호
){}

/*
    [고려]
    contract는 Json 그대로 넘기기 때문에
    필드명이 바뀌면 추적이 어려움.
    구조가 어느
 */