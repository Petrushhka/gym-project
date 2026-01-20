package com.gymproject.user.membership.domain.event;

import com.gymproject.common.event.domain.IdentityRoleAction;
import com.gymproject.common.event.domain.UserRoleChangedEvent;
import com.gymproject.user.membership.domain.type.MembershipChangeType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipEventTranslator {

    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void translate(MembershipChangedEvent internalEvent) {
        // 1. 멤버십 변경 행위가 어떤 것인지 결정
        IdentityRoleAction action = determineAction(internalEvent.getType());

        // 2. Auth 모듈이 이해할 수 있는 언어로 번역해서 발송
        eventPublisher.publishEvent(
                new UserRoleChangedEvent(
                        internalEvent.getUserMembership().getUser().getUserId(),
                        action
                )
        );
    }

    private IdentityRoleAction determineAction(MembershipChangeType type) {
        return switch (type) {
            case PURCHASE, EXTEND, RESUME -> IdentityRoleAction.PROMOTE;
            case EXPIRED, ROLLBACK, CANCELLED -> IdentityRoleAction.DEMOTE;
            default -> throw new IllegalArgumentException("알 수 없는 멤버십 변경 타입입니다.");
        };
    }

}
