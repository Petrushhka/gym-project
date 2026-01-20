package com.gymproject.auth.domain.event;

import com.gymproject.auth.domain.entity.Identity;
import com.gymproject.common.event.domain.ProfileInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IdentityCreatedEvent {
    private final Identity identity;
    private final ProfileInfo profile;


    public static IdentityCreatedEvent create(Identity identity, ProfileInfo profile) {
        return new IdentityCreatedEvent(
                identity, // 참조로 전달(null 값 전달될 수 도 있음. INSERT전에 이벤트 발생이라)
                profile
        );
    }
}
/*
    가입시 User 엔티티에 넘겨줘야할 속성과 User 엔티티의 고유번호
    성, 이름, 성별, 휴대폰 번호 등
 */