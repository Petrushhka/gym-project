package com.gymproject.auth.infrastructure.listener;

import com.gymproject.auth.application.service.IdentityService;
import com.gymproject.common.event.domain.UserRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityRoleChangedListener {
    private final IdentityService identityService;

    @EventListener
    public void handle(UserRoleChangedEvent event) {
        identityService.changeMembership(
                event.userId(),
                event.action()
        );
    }

}
