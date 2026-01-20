package com.gymproject.classmanagement.schedule.domain.event;

import com.gymproject.classmanagement.schedule.domain.entity.Schedule;
import com.gymproject.classmanagement.template.domain.entity.Template;
import com.gymproject.common.event.domain.ScheduleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleEventTranslator {

    private final ApplicationEventPublisher applicationEventPublisher;

    // 트랜잭션이 성공적으로 커밋된 후에만 외부 이벤트를 발행함
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void translate(ScheduleChangedEvent internalEvent) {
        log.info("📢 [이벤트 도달] 스케줄 ID: {}", internalEvent.getSchedule().getClassScheduleId());
        // Schedule 정보 가져오기
        Schedule schedule = internalEvent.getSchedule();
        log.info("Step 1: 엔티티 접근 성공");

        // Template Title 만들기
        Template template = schedule.getTemplate();
        log.info("Step 2: 템플릿 접근 성공 (Title: {})", template != null ? template.getTitle() : "N/A");

        String title = "1:1 PT"; // 기본 값으로 1:1예약으로 설정
        Long totalCapacity = 1L; // 기본값으로 1:1 예약의 인원으로 설정

        if (template != null) { // 템플릿이 있고, title이 비어있지 있으면
            title = template.getTitle();
            totalCapacity = (long) template.getCapacity();
        }

        // 3. 현재 예약된 인원 계산( 총원 - 잔여석)
        long bookedCount = totalCapacity - schedule.getCapacity();

        // 외부 이벤트객체로 매핑
        ScheduleEvent externalEvent =
                new ScheduleEvent(
                        schedule.getClassScheduleId(),
                        schedule.getTrainerId(),
                        schedule.getStartAt(),
                        schedule.getEndAt(),
                        schedule.getStatus().name(),
                        title, // 타이틀을 이벤트로 실어서 보내는중 , 1:1 PT예약일시 null 값으로 보내야함
                        totalCapacity, // 총 인원(추후 데이터타입 수정)
                        bookedCount
                );
        log.info("Step 3: 외부 이벤트 발행 직전");
        // 이벤트 발행
        applicationEventPublisher.publishEvent(externalEvent);
        log.info("✅ Step 4: 외부 이벤트 발행 완료");
    }
}
