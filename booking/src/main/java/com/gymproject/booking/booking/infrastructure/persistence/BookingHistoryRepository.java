package com.gymproject.booking.booking.infrastructure.persistence;

import com.gymproject.booking.booking.domain.entity.BookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookingHistoryRepository extends JpaRepository<BookingHistory, Long>,
        JpaSpecificationExecutor<BookingHistory> {
}

/*
   BookingHistory가 이벤트가 발생이 안하는줄 알았지만
   @PostPersist에서 멀쩡,
   Listener에서도 객체를 확인하였음.

   그런데 DB에만 저장되지 않는 상황임.

   원인은 AFTER_COMMIT에서 JPA 영속성 컨텍스트가 이미 닫혀버린 상태임.

    TransactionPhase.AFTER_COMMIT 흐름:
    Service @Transactional -> Commit 됨
                    ↓
    트랜잭션 종료
                    ↓
    AFTER_COMMIT Listener 실행
                    ↓
    현재는 트랜잭션이 종료된 상태 -> 새로운 트랜잭션을 만들지 않으면 DB 반영이 되지 않음.

    Listener가 트랜잭션 밖에서 실행되기 때문에 save()가 flush되지 않음.


    방법은 두가지이지만, 스프링 공식 문서에서 권장하는 방법은 다음과 같음.

    @Transactional(propagation = Propagation.REEQUIRES_NEW)
    를 추가 하여 Listener를 별도 트랜잭션으로 실행시키는 것임!




 */