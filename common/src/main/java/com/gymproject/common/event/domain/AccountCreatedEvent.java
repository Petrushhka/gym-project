package com.gymproject.common.event.domain;

public record AccountCreatedEvent(
        Long identityId,
        ProfileInfo profileInfo
) {
}

/*
    [중요]
    IdentityCreatedEvent와 모양이 같은 클래스이지만,
    해당 이벤트는외부용 이벤트로 USer모듈에게 전달할 내용임.
    내부이벤트는 나중에 다른 형태로 필드가 채워질수있음(로그인 시도 횟수, 가입 Ip 등)
 */