package com.gymproject.readmodel.infrastructure.listener;

import com.gymproject.common.event.domain.ScheduleEvent;
import com.gymproject.readmodel.application.TrainerCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleEventListener {

    private final TrainerCalendarService trainerCalendarService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @EventListener
    public void handle(ScheduleEvent event) {
        trainerCalendarService.synchronizeSchedule(event);
        log.info("📥 [ReadModel 수신] 스케줄 동기화 시작: ID={}, Type={}",
                event.scheduleId(), event.status());

        try {
            trainerCalendarService.synchronizeSchedule(event);
            log.info("✅ [ReadModel 완료] 스케줄 동기화 성공: ID={}", event.scheduleId());
        } catch (Exception e) {
            log.error("❌ [ReadModel 에러] 동기화 중 실패: ID={}, 이유={}",
                    event.scheduleId(), e.getMessage(), e);
        }
    }
}
