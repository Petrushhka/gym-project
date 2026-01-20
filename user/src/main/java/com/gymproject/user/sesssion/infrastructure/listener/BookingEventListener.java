//package com.gymproject.user.sesssion.infrastructure.listener;
//
//import com.gymproject.common.event.domain.BookingEvent;
//import com.gymproject.user.sesssion.application.UserSessionService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.event.TransactionPhase;
//import org.springframework.transaction.event.TransactionalEventListener;
//
///*
//    사용자가 직접 취소했을 때 담당하는 리스너임
// */
//@Component
//@RequiredArgsConstructor
//public class BookingEventListener {
//
//    private final UserSessionService userSessionService;
//
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void handle(BookingEvent event) {
//        // 1. 취소 이벤트인지 확인
//        if(!event.status().equalsIgnoreCase("CANCEL")){
//            return;
//        }
//
//        // 2. 세션 복구가 필요한 상황인지 확인
//        if(event.cancellationType().equalsIgnoreCase("FREE_CANCEL") &&
//        event.userSessionId() != null){
//            userSessionService.restore(event.userSessionId());
//        }
//
//        if(event.userSessionId() == null) return;
//    }
//
//}


/*
    [즁요****************]
    Session권의 복구는 강한 트랜잭션으로 묶여야하기 때문에 이벤트로 하지않고 바로 포트로 Session을 건드리는것으로하였음.
    따라서 해당 리스너는 Deprecate되었음.
 */