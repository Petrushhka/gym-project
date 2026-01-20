package com.gymproject.user.sesssion.infrastructure.listener;

import com.gymproject.user.profile.domain.event.UserJoinedEvent;
import com.gymproject.user.sesssion.application.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserJoinedEventListener {

    private final UserSessionService userSessionService;

    /*
        AFTER_COMMIT은 준영속 상태임.
        User가 DB에 등록되고나서 부터는 JPA에 해당 엔티티를 감시하고 있지 않는다는 것임.
        하지만 나의 경우는 User에 대한 Id 정보만 있으면되기 때문에 상관없어서 Select 쿼리문을 줄인거임

        이렇게 안햇으면 UserId를 다시 넘겨서 여기서 Id를 가지고 Session을 조회해야하는 번거로운 일이 반복됨.
     */

    /*
        @Transactional의 기본값은 트랜잭션이 없으면 새로 만드는게 맞음.
        하지만 AFTER_COMMIT 리스너 안에서는 "트랜잭션 컨텍스트는 남아잇지만, 이미 커밋되어서 더 이상 쓸 수 없느 상태"로 인식될 때가 많음
        그래서 재활용하지말고 무조건 새로 트랜잭션을 강제해야함.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserJoinedEvent event) {
        log.info("UserJoinedEvent received: {}", event);
        userSessionService.giveFreeTrialSession(event.getUser(), event.getJoinDate());

    }
}
