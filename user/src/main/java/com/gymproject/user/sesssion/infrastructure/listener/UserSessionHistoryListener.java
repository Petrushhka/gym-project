package com.gymproject.user.sesssion.infrastructure.listener;

import com.gymproject.user.sesssion.domain.entity.UserSession;
import com.gymproject.user.sesssion.domain.entity.UserSessionHistory;
import com.gymproject.user.sesssion.domain.event.SessionChangedEvent;
import com.gymproject.user.sesssion.domain.type.SessionChangeType;
import com.gymproject.user.sesssion.infrastructure.persistence.UserSessionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserSessionHistoryListener {

    private final UserSessionHistoryRepository historyRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordHistory(SessionChangedEvent event) {
        // 1. 이벤트에서 엔티티를 바로 꺼냄 (repository 조회 안해도됨)
        UserSession session = event.getUserSession();

        // 2. 히스토리 객체 생성
        // 엔티티가 이미 '사용' 혹은 '복구' 된 상태이므로 session.getUSedSession() 은 최신값임
        UserSessionHistory history =
                switch(event.getType()){
                    case USE -> UserSessionHistory.recordUse(
                            session,
                            event.getType().getDescription(),
                            session.getSessionType(),
                            event.getModifier()
                    );
                    case PURCHASE -> UserSessionHistory.recordPurchase(
                            session,
                            event.getType().getDescription(),
                            session.getSessionType(),
                            event.getModifier()
                    );
                    case RESTORE -> UserSessionHistory.recordRestore(
                            session,
                            event.getType().getDescription(),
                            session.getSessionType(),
                            event.getModifier()
                    );
                    case REFUNDED ->  UserSessionHistory.recordRefund(
                            session,
                            event.getAmount(),
                            event.getType().getDescription(),
                            session.getSessionType(),
                            event.getModifier()
                    );
                    case DEACTIVATED ->   UserSessionHistory.recordDeactivate(
                            session,
                            event.getModifier()
                    );
                    case EXPIRED -> UserSessionHistory.recordExpire(
                            session,
                            event.getModifier(),
                            event.getAmount()
                    );
                };

        // 3. 저장
        historyRepository.save(history);
    }

}
/**
 * Spring Event 사용 주석
 * @see com.gymproject.user.sesssion.domain.event.SessionChangedEvent
 */

/* -----------------------------------------과거 코드 --------------------------
/**
 * @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
 *
 * [SERVICE} userSession.useSession()
 * [SERVICE] publishEvent() 이벤트 발행
 * -> 리스너가 바로 실행되지 않고 대기 상태에 있음(서비스의 트랜잭션이 안끝났으므로)
 * [SERVICE] 메서드 종료(트랜잭션 커밋하려고함)
 * [Spring] 대기하고 있던 BEFORE_COMMIT 리스너 호출
 * [LISTNER] historyRepository.save() 실행 (save()는 영속성 컨텍스트에만 변경사항 등록)
 * [DB] 최종 커밋
 *
 * 서비스 로직이 실패했으면? -> BEFORE_COMMIT 실행(리스너 실행 안됨)
 * 리스너(히스토리 저장)이 실패했으면? -> BEFORE COMMIT 단계에서 예외 발생, 트랜잭션 전체가 롤백, 서비스+히스토리 로직 둘 다 롤백
 *
 * 강력한 데이터 정합성 보장
 *
 *
 * IMMEDIATE - (publishEvent() 호출시 바로 실행)리스너 실패하자마자 서비스도 실패
 * BEFORE_COMMIT - (트랜잭션 커밋 직전에 실행)리스너 실패 시 서비스도 롤백
 * AFTER_COMMIT - (트랜잭션 커밋 완료 후 실행)리스너 실패 . 서비스 영향 없음
 * AFTER_ROLLBACK - (롤백된 후 실행)서비스 실패 시만 실행

@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
public void recordHistory(SessionChangedEvent event) {
    // 1. 엔터티 조회
    UserSession session = userSessionRepository.findById(event.getUserSessionId())
            .orElseThrow(()->new IllegalArgumentException("세션정보를 찾을 수 없습니다."));

    UserSessionHistory history = null;

    // 2. 이벤트 타입에 따라 알맞은 팩토리 메서드 분기
    // 세션권 사용
    if(event.getType() == SessionChangeType.USE) {
        history = UserSessionHistory.recordUse(
                session,
                event.getDescription(),
                event.getSessionType()
        );
    }
    // 세션권 환불
    else if(event.getType() == SessionChangeType.REFUND){
        history = UserSessionHistory.recordRefund(
                session,
                event.getDescription(),
                event.getSessionType()
        );
    }
    // 세션권 구매
    else if(event.getType() == SessionChangeType.PURCHASE){
        history = UserSessionHistory.recordPurchase(
                session,
                event.getDescription(),
                event.getSessionType()
        );
    }

    // 3. 저장
    if(history != null) {
        historyRepository.save(history);
    }
}

*/



/*  [어댑터]
   서비스계층에서 로직을 돌리지 않고 왜 어댑터에서 로직을 다 수행하느냐?
    UserSessionHistoryListner는 상태변경(내부 이벤트임)
    내 모듈 안에서 수정이 완료됬다고 알려줌.
    이미 끝난 일에 대해 부가 작업(기록) 수행
    복잡도 낮음

    SessionPaymentListener는 외부이벤트
    새로운 비지니스 시나리오 시작
    복잡도 높음
 */
