package com.gymproject.user.membership.application.dto;

import java.time.OffsetDateTime;

public record MembershipContract(
        OffsetDateTime startDate
) {
}
