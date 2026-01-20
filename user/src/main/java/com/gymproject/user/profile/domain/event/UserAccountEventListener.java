package com.gymproject.user.profile.domain.event;

import com.gymproject.common.event.domain.AccountCreatedEvent;
import com.gymproject.common.event.domain.ProfileInfo;
import com.gymproject.user.profile.application.UserProfileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAccountEventListener {

    private final UserProfileService userProfileService;

    @EventListener
    @Transactional
    public void handle(AccountCreatedEvent event){
        // 1. 이벤트에서 정보꺼내기
        Long userId = event.identityId();
        ProfileInfo profileInfo = event.profileInfo();

        // 2. 서비스 로직에서 유저 저장
        userProfileService.registUser(userId, profileInfo);

    }

}
