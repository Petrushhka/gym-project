package com.gymproject.user.sesssion.domain.event;

import com.gymproject.user.sesssion.domain.entity.UserSession;
import com.gymproject.user.sesssion.domain.type.SessionChangeType;
import com.gymproject.common.vo.Modifier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SessionChangedEvent {

    private final UserSession userSession; // 유저 세션 객체
    private final Modifier modifier; // 변경 주체 (시스템, 관리자, 사용자)
    private final SessionChangeType type; // 구매, 환불, 사용, 만료 등
    private final int amount; // 사용량

    // 정보를 조립하는 빌더 클래스
    public static class Builder{
        private final UserSession userSession;
        private final Modifier modifier;
        private SessionChangeType type;
        private int amount;

        public Builder(UserSession userSession, Modifier modifier) {
            this.userSession = userSession;
            this.modifier = modifier;
        }

        public Builder action(SessionChangeType type, int amount){
            this.type = type;
            this.amount = amount;
            return this;
        };

        public SessionChangedEvent build(){
            return new SessionChangedEvent(this);
        }
    }

    // 내부 생성자는 Builder를 통해서만 호출
    private SessionChangedEvent(Builder builder){
        this.userSession = builder.userSession;
        this.modifier = builder.modifier;
        this.type = builder.type;
        this.amount = builder.amount;
    }

    /**  --------------- 정적 팩토리 메서드 ---------------- **/

    // 세션 사용(차감)
    public static SessionChangedEvent used(UserSession userSession, Modifier modifier){
        return new Builder(userSession, modifier)
                .action(SessionChangeType.USE, -1)
                .build();
    }

    // 세션 환불(복구)
    public static SessionChangedEvent restored(UserSession userSession, Modifier modifier){
        return new Builder(userSession, modifier)
                .action(SessionChangeType.RESTORE, 1)
                .build();
    }

    // 세션 구매(충전)
    public static SessionChangedEvent purchased(UserSession userSession, Modifier modifier, int amount){
        return new Builder(userSession, modifier)
                .action(SessionChangeType.PURCHASE, amount)
                .build();
    }

    // 세션 만료(기간 만료)
    public static SessionChangedEvent expired(UserSession userSession, Modifier modifier, int amount){
        return new Builder(userSession, modifier)
                .action(SessionChangeType.EXPIRED, amount)
                .build();
    }

    // 세션 중지(사용 정지, 회원 멤버십 종료로 인한)
    public static SessionChangedEvent deactivated(UserSession userSession, Modifier modifier){
        return new Builder(userSession, modifier)
                .action(SessionChangeType.DEACTIVATED, 0)
                .build();
    }

    // 세션권 환불(환불 요청으로 환불)
    public static SessionChangedEvent refunded(UserSession userSession, Modifier modifier, int amount){
        return new Builder(userSession, modifier)
                .action(SessionChangeType.REFUNDED, amount)
                .build();
    }

    /*  ---------------- 과거 기록 --------------------------
    // 사용(차감) 이벤트
    public static SessionChangedEvent use(Long sessionId, Long userId, int remain, SessionType sessionType) {
        return new SessionChangedEvent(
                sessionId,userId,SessionChangeType.USE,-1,remain, "수업 예약 차감", sessionType
        );
    }

    // 복구(환불) 이벤트 팩토리
    public static SessionChangedEvent refund(Long sessionId, Long userId, int remain, SessionType sessionType) {
        return new SessionChangedEvent(
                sessionId,userId,SessionChangeType.REFUND,1, remain,"수업 취소 환불", sessionType
        );
    }

    // 구매 이벤트
    // amount: 충전된 횟수(eg. 10)
    // remain: 남은 횟수(구매 직후라서 total과 같음)
    // description: 세션권 횟수
    public static SessionChangedEvent purchase(Long sessiondId, Long userId,
                                               int amount, int remain, int description, SessionType sessionType) {
        return new SessionChangedEvent(
                sessiondId,
                userId,
                SessionChangeType.PURCHASE,
                amount,
                remain,
                "PT 세션구매: " + description,
                sessionType
        );
    }
        */
}


/*
    USER_SESSION_TB: 현재 Session 상태 관리 DB
    USER_SESSION_HISTORY_TB: Session log를 남기는 DB

    따라서 USER_SESSION_TB가 업데이트 될때마다 이벤트 발행
    LISTENER를 통해 HISTORY_TB에 기록

 */


/**
    Spring Event는 단순한 자바객체(POJO)임
    DTO와 같은 데이터 전달 객체 역할을 함.

    Spring Event는 특별한 부모 클래스를 상속할 필요도 없음
    과거에는 ApplicationEvent를 상속했지만 지금은 필수가 아님.

    1. Event -> '이런 일이 발생했다'라는 정보를 다음 메세지 객체

   2.  이벤트 객체를 만든 후, publishEvent(...) 을 이용해서 스프링에 전달.

   3. Spring Context에 등록된 Bean 중에서 @EventListener(파라미터 타입 = 발행된 이벤트 타입) 을 가진 메서드를 찾아냄
      동일한 파라미터의 Listener 메서드를 찾는거임


      다시 정리

      1. Event 역할의 단순 POJO 객체 정의
      (DTO와 거의 동일한 데이터 전달 객체)

      2. 서비스 뢰직에서 그 Event 객체를 생성
      applicationEventPublisher.publishEvent(event)를 호출

      3. Spring Context에서 Bean을 찾음(@EventListener 또는 @TransactionalEventListener)
         이벤트 타입을 파라미터로 받는 메서드를 찾아냄.

      4. 찾은 Listener 메서드를 자동으로 호출하여 해당 메서드의 파라미터로 이벤트 객체를 넘겨줌.
 */