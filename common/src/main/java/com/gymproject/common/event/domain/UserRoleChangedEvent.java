package com.gymproject.common.event.domain;

public record UserRoleChangedEvent(
        Long userId,
        IdentityRoleAction action
) {
}
