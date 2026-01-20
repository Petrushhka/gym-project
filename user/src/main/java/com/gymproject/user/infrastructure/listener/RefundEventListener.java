package com.gymproject.user.infrastructure.listener;

import com.gymproject.common.event.integration.RefundEvent;
import com.gymproject.common.security.Roles;
import com.gymproject.common.vo.Modifier;
import com.gymproject.user.membership.application.UserMembershipService;
import com.gymproject.user.sesssion.application.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RefundEventListener {

    private final UserMembershipService userMembershipService;
    private final UserSessionService userSessionService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(RefundEvent event) {

        // 1. Modifier 설정
        Modifier modifier = resolveModifier(event);

        // 2. 카테고리 별로 서비스 호출
        if (event.category().equalsIgnoreCase("MEMBERSHIP")) {
            userMembershipService.refundMembership(event.sourceId(), modifier);
        }
        if (event.category().equalsIgnoreCase("SESSION")) {
            userSessionService.refundSession(event.sourceId(), modifier);
        }

    }

    private Modifier resolveModifier(RefundEvent event) {
        // 트레이너
        if (event.actorRole() == Roles.TRAINER) return Modifier.admin(event.actorId(), "트레이너");
        // 회원
        if (event.actorRole() == Roles.MEMBER ||
                event.actorRole() == Roles.GUEST) return Modifier.user(event.actorId(), "회원");
        // 시스템
        return Modifier.system();
    }
}
