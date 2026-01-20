package com.gymproject.user.membership.infrastructure.adapter;

import com.gymproject.common.port.user.UserMembershipPort;
import com.gymproject.user.membership.application.UserMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class UserMembershipAdapter implements UserMembershipPort {

    private final UserMembershipService userMembershipService;

    @Override
    public OffsetDateTime resolveExtensionStartAt(Long userId, OffsetDateTime now) {

        return userMembershipService.calculateExtensionStartAt(userId, now);

    }

    @Override
    public void validateMembershipUntil(Long userId, OffsetDateTime requiredDate) {
            userMembershipService.validateActiveUntil(userId, requiredDate);
        }
}
