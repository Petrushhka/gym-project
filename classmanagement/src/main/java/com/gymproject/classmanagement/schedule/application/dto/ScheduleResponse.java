package com.gymproject.classmanagement.schedule.application.dto;

import com.gymproject.classmanagement.schedule.domain.entity.Schedule;
import com.gymproject.classmanagement.schedule.domain.type.ScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
@Schema(description = "개별 수업 일정 정보 응답")
public class ScheduleResponse {
    @Schema(description = "수업 스케줄 고유 ID", example = "7001")
    private Long scheduleId;

    @Schema(description = "수업 제목", example = "아침 요가 루틴")
    private String title;          // 수업 제목

    @Schema(description = "수업 시작 시간 (브리즈번 기준 ISO-8601)", example = "2026-02-01T10:00:00+10:00")
    private OffsetDateTime start;  // 시작 시간 (Full Calendar 등 라이브러리 포맷)

    @Schema(description = "수업 종료 시간 (브리즈번 기준 ISO-8601)", example = "2026-02-01T11:00:00+10:00")
    private OffsetDateTime end;    // 종료 시간

    @Schema(description = "수업 상태 (OPEN: 예약가능, CLOSED: 마감, CANCELLED: 폐강)", example = "OPEN")
    private ScheduleStatus status; // OPEN, CLOSED, RESERVED...

    @Schema(description = "수업 총 정원", example = "10")
    private int capacity;          // 총 정원 (Template 정보 혹은 1:1)

    @Schema(description = "현재 예약된 인원", example = "3")
    private int currentBooked;     // 현재 예약된 인원 (총원 - 잔여석)

    public static ScheduleResponse create(Schedule schedule) {
        // 템플릿이 없으면(1:1) 기본 제목 사용
        String title = (schedule.getTemplate() != null)
                ? schedule.getTemplate().getTitle()
                : "1:1 퍼스널 트레이닝";

        int totalCapacity = (schedule.getTemplate() != null)
                ? schedule.getTemplate().getCapacity()
                : 1;

        return ScheduleResponse.builder()
                .scheduleId(schedule.getClassScheduleId())
                .title(title)
                .start(schedule.getStartAt())
                .end(schedule.getEndAt())
                .status(schedule.getStatus())
                .capacity(totalCapacity)
                .currentBooked(totalCapacity - schedule.getCapacity()) // 예약된 수 계산
                .build();
    }
}
