package com.gymproject.auth.domain.event;

import com.gymproject.common.event.domain.AccountCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityEventTranslator {

    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void translate(IdentityCreatedEvent internalEvent) {
        // 내부 이벤트에서, 외부 이벤트로 재 발행
        eventPublisher.publishEvent(
                new AccountCreatedEvent(
                        internalEvent.getIdentity().getIdentityId(),
                        internalEvent.getProfile()
                )
        );

    }
}
