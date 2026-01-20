package com.gymproject.classmanagement.schedule.domain.service;

import com.gymproject.classmanagement.recurrence.domain.entity.RecurrenceGroup;
import com.gymproject.classmanagement.schedule.domain.entity.Schedule;
import com.gymproject.classmanagement.template.domain.entity.Template;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScheduleGenerator {

    public List<Schedule> generateSchedules(RecurrenceGroup group, Template template) {
        List<Schedule> results = new ArrayList<>();

        LocalDate startDate = group.getStartDate(); // 반복 시작일 (예: 2025-12-01)
        LocalDate endDate = group.getEndDate(); // 반복 종료일 (예: 2026-02-28)
        LocalTime startTime = group.getStartTime(); // 순수한 시각 (예: 10:00:00)

        // TimezoneId가 유효한지 검사
        ZoneId zoneId = ZoneId.of(group.getTimezoneId());
        // 시작일부터 종료일까지 하루씩 증가
        while (!startDate.isAfter(endDate)) {
            // 요일 일치 여부 확인 -> getDayOfWeek() 하면 특정 날짜를 요일 ENum으로 바꿔줌. 출력형태는 SUNDAY...
            if (group.getRepeatDays().contains(startDate.getDayOfWeek())) {

                // 1. Local Date/Time + ZoneId = OffsetDateTime 생성
                ZonedDateTime zdtStart = ZonedDateTime.of(startDate, startTime, zoneId);
                // 2. 절대 시점(OffsetDateTime)으로 변환
                OffsetDateTime startAt = zdtStart.toOffsetDateTime();
                // 수업 종료시간까지 계산
                OffsetDateTime endAt = startAt.plusMinutes(group.getTemplate().getDurationMinutes());

                // 3. 엔티티 생성 -> 빌더 패턴을 정적 팩토리 메서드로
//                Schedule schedule = Schedule.builder()
//                        .trainerId(group.getTrainerId())
//                        .template(template)
//                        .startAt(startAt)
//                        .endAt(endAt)
//                        .status(ScheduleStatus.OPEN) // 초기 상태
//                        .capacity(template.getCapacity())
//                        .recurrenceGroup(group)
//                        .build();
                Schedule schedule = Schedule.createRecurrence(
                        group, template, startAt, endAt
                );

                results.add(schedule);
            }
            startDate = startDate.plusDays(1);
        }
        return results;

    }
}

/* 기존 코드는 아래와 같음

private List<Schedule> expandSchedules(RecurrenceGroup group, Template template) {
        List<Schedule> results = new ArrayList<>();

        LocalDate startDate = group.getStartDate(); // 반복 시작일 (예: 2025-12-01)
        LocalDate endDate = group.getEndDate(); // 반복 종료일 (예: 2026-02-28)
        LocalTime startTime = group.getStartTime(); // 순수한 시각 (예: 10:00:00)

        // TimezoneID가 유효한지 검사하는 것이 안전합니다.
        ZoneId zoneId = ZoneId.of(group.getTimezoneId());

        // 시작일부터 종료일까지 하루씩 증가
        while (!startDate.isAfter(endDate)) {
            // 요일 일치 여부 확인 -> getDayOfWeek() 하면 특정 날짜를 요일 ENum으로 바꿔줌. 출력형태는 SUNDAY...
            if (group.getRepeatDays().contains(startDate.getDayOfWeek())) {

                // 1. Local Date/Time + ZoneId = OffsetDateTime 생성
                ZonedDateTime zdtStart = ZonedDateTime.of(startDate, startTime, zoneId);

                // 2. 절대 시점(OffsetDateTime)으로 변환
                OffsetDateTime startAt = zdtStart.toOffsetDateTime();
                // 수업 종료시간까지 계산
                OffsetDateTime endAt = startAt.plusMinutes(group.getTemplate().getDurationMinutes());

                // 3. 엔티티 생성
                Schedule schedule = Schedule.builder()
                        .trainerId(group.getTrainerId())
                        .template(template)
                        .startAt(startAt)
                        .endAt(endAt)
                        .status(ScheduleStatus.OPEN) // 초기 상태
                        .capacity(template.getCapacity())
                        .recurrenceGroup(group)
                        .build();

                results.add(schedule);
            }
            startDate = startDate.plusDays(1);
        }
        return results;
    }

 */

