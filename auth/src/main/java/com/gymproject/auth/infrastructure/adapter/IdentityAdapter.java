package com.gymproject.auth.infrastructure.adapter;

import com.gymproject.auth.application.service.IdentityService;
import com.gymproject.common.port.auth.IdentityQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityAdapter implements IdentityQueryPort {

    private final IdentityService identityService;

    @Override
    public void validateTrainer(Long trainerId) {
        identityService.validateTrainer(trainerId);
    }

    @Override
    public void validateMembershipUser(Long userId) {
        identityService.validateMember(userId);
    }
}
