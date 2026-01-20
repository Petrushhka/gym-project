package com.gymproject.readmodel.application;

import com.gymproject.readmodel.domain.CalendarStatus;
import com.gymproject.readmodel.domain.TrainerCalendar;
import com.gymproject.readmodel.domain.type.CalendarSource;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

@Schema(description = "트레이너 통합 일정 응답 (수업 및 개인 일정 포함)")
public record TrainerCalendarResponse(
        @Schema(description = "캘린더 항목 고유 ID", example = "1001")
        Long calendarId,

        @Schema(description = "원본 데이터 ID (해당 ID로 수업/휴무 상세 조회 가능)", example = "500")
        Long sourceId, // 원본 Id (클릭 시 상세조회용)

        @Schema(description = "일정 소스 타입 (SCHEDULE: 정규 수업, TIME_OFF: 트레이너 휴무)", example = "SCHEDULE")
        CalendarSource type, // 스케쥴인지, 휴무인지

        @Schema(description = "일정 제목", example = "개인 레슨")
        String title,

        @Schema(description = "일정 상태 (RESERVED_TIME: 개인 수업, CLASS_OPEN: 그룹수업 예약가능 등)", example = "RESERVED_TIME")
        CalendarStatus status, // 예약가능한 수업인지

        @Schema(description = "일정 시작 시간 (호주 브리즈번 기준 ISO-8601)", example = "2026-01-18T10:00:00+11:00")
        OffsetDateTime start, // 시작 시간

        @Schema(description = "일정 종료 시간 (호주 브리즈번 기준 ISO-8601)", example = "2026-01-18T11:00:00+11:00")
        OffsetDateTime end // 종료 시간
){

    public static TrainerCalendarResponse create(TrainerCalendar calendar) {
       // 1. Range<ZondedDateTime> 에서 ZonedDateTime 추출 [중요]
        ZonedDateTime startTime = calendar.getTimeRange().lower();
        ZonedDateTime endTime = calendar.getTimeRange().upper();

        return new TrainerCalendarResponse(
                calendar.getCalendarId(),
                calendar.getSourceId(),
                calendar.getSourceType(),
                calendar.getTitle(),
                calendar.getStatus(),
                startTime != null ? startTime.toOffsetDateTime() : null,
                endTime != null ? endTime.toOffsetDateTime() : null
        );
    }
}

/*
    JSON 표준 호환성: 대부분의 프론트엔드 라이브러리는 2026-01-18T10:00:00+11:00 같은 ISO-8601 형식을 기대합니다. ZonedDateTime을 그대로 직렬화하면 [Australia/Sydney] 같은 지역 정보가 붙어 파싱이 번거로워질 수 있습니다.

    클라이언트의 단순화: 프론트엔드는 "시드니 시간인지"보다 "현재 시점의 오프셋이 얼마인지"만 알면 현지 시간으로 정확히 렌더링할 수 있습니다.

    데이터 무결성: toOffsetDateTime()을 호출하면 시드니의 **DST(일광 절약 시간제)가 반영된 당시의 오프셋(+10 또는 +11)**이 그대로 유지된 채 변환됩니다.

    postgre에서 tstzrange는 한쪽이 열려있는 무한대 범위를 가질수있음
    그래서 상한선이 없으면 upper()는 null을 반환할 가능성이있음
    따라서 null 체크를 해주는게 안전
 */