package com.gymproject.user.profile.infrastructure.adapter;

import com.gymproject.common.port.user.UserProfilePort;
import com.gymproject.user.profile.application.UserProfileService;
import com.gymproject.user.profile.domain.vo.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserProfileAdapter implements UserProfilePort {

    private final UserProfileService userProfileService;

    @Override
    public Long findIdentityIdByPhone(String phoneNumber) {
        // 사용자 입력 문자열 -> VO -> Repository
        PhoneNumber phoneVO = new PhoneNumber(phoneNumber);

        return userProfileService.findUserIdByPhone(phoneVO);
    }

    @Override
    public String getUserFullName(Long trainerId) {
        return userProfileService.getUserFullName(trainerId);
    }

    @Override
    public void checkDuplicatePhoneNumber(String phoneNumber) {
        // VO로 변경
        PhoneNumber phoneVO = new PhoneNumber(phoneNumber);

        userProfileService.checkDuplicatePhoneNumber(phoneVO);
    }
}
