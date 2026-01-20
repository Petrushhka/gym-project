package com.gymproject.user.membership.infrastructure.listener;

import com.gymproject.user.membership.domain.entity.UserMembershipHistory;
import com.gymproject.user.membership.domain.event.MembershipChangedEvent;
import com.gymproject.user.membership.domain.type.MembershipChangeType;
import com.gymproject.user.membership.infrastructure.persistence.UserMembershipHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MembershipHistoryListener {

    private final UserMembershipHistoryRepository membershipHistoryRepository;

    /*
        멤버십 상태 변경 이벤트를 감지하여 히스토리에 저장
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(MembershipChangedEvent event) {

        UserMembershipHistory history =
                switch (event.getType()) {
                    case MembershipChangeType.PURCHASE -> UserMembershipHistory.recordPurchase(
                            event.getUserMembership(),
                            event.getModifier()
                    );

                    case MembershipChangeType.EXTEND ->  UserMembershipHistory.recordExtend(
                            event.getUserMembership(),
                            event.getBeforeExpiredDate(),
                            event.getModifier()
                    );
                    case MembershipChangeType.ROLLBACK ->  UserMembershipHistory.recordRollback(
                            event.getUserMembership(),
                            event.getBeforeExpiredDate(),
                            event.getModifier()
                    );
                    case MembershipChangeType.EXPIRED -> UserMembershipHistory.recordExpire(
                            event.getUserMembership(),
                            event.getModifier()
                    );
                    case MembershipChangeType.SUSPEND ->   UserMembershipHistory.recordSuspend(
                            event.getUserMembership(),
                            event.getModifier(),
                            event.getDescription()
                    );
                    case MembershipChangeType.RESUME ->    UserMembershipHistory.recordResume(
                            event.getUserMembership(),
                            event.getBeforeExpiredDate(),
                            event.getAmountDays(),
                            event.getModifier()
                    );
                    case MembershipChangeType.CANCELLED -> UserMembershipHistory.recordCancel(
                            event.getUserMembership(),
                            event.getBeforeExpiredDate(),
                            event.getModifier()
                    );
                };

                membershipHistoryRepository.save(history);

    }

}
