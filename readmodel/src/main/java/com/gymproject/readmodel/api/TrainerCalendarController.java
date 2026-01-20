package com.gymproject.readmodel.api;

import com.gymproject.common.dto.exception.CommonResDto;
import com.gymproject.readmodel.application.TrainerCalendarResponse;
import com.gymproject.readmodel.application.TrainerCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static com.gymproject.common.constant.GymTimePolicy.SERVICE_ZONE;

@Tag(name = "4. 일정 조회", description = "통합 일정 조회")

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/calendars")
public class TrainerCalendarController {

    private final TrainerCalendarService trainerCalendarService;

    @Operation(
            summary = "1. 트레이너 통합 일정 조회",
            description = """
            특정 기간 내의 모든 일정(수업, 개인 일정, 강좌)을 조회합니다.
            
            1. 프론트엔드에서 받은 `LocalDate`를 호주 브리즈번(`Australia/Brisbane`) 타임존의 시작 시각(00:00:00)과 종료 시각(23:59:59)으로 변환합니다.
            2. DB의 `timestamptz` 및 `tstzrange` 데이터와 비교하여 정확한 범위 내의 일정을 추출합니다.
            3. DST(썸머타임)가 적용된 정확한 `OffsetDateTime` 결과를 반환합니다.
            """
    )
    @GetMapping
    public ResponseEntity<CommonResDto<List<TrainerCalendarResponse>>> getAllTrainerCalendars(
            @Parameter(description = "트레이너 고유 ID")@RequestParam Long trainerId,
            @Parameter(description = "조회 시작일 (기본값: 당월 1일)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate start,
            @Parameter(description = "조회 종료일 (기본값: 당월 말일)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate end
            ) {

        // 기본 값 설정
        if(start == null){
            start = LocalDate.now(SERVICE_ZONE).withDayOfMonth(1);
        }
        if(end == null){
            // start가 속한 달의 마지막 날짜(30,31 자동 계산)
            end = start.withDayOfMonth(start.lengthOfMonth());
        }
        // 시작일 00:00:00
        OffsetDateTime startDateTime = start.atStartOfDay(SERVICE_ZONE).toOffsetDateTime();
        // 종료일 23:59:59:999
        OffsetDateTime endDateTime = end.atTime(LocalTime.MAX).atZone(SERVICE_ZONE).toOffsetDateTime();

        List<TrainerCalendarResponse> result = trainerCalendarService.getCalendar(trainerId, startDateTime, endDateTime);

        return ResponseEntity.ok(
                CommonResDto.success(200, "일정이 성공적으로 조회되었습니다.", result)
        );
    }
}
/*
    Date , DateTime, Offset/zoned
    1. 달력에 보여주는 API는 LocalDate을 받아서 DB의 시간(OffsetDateTIme/Range)를 조회해햐함.

    프론트: 2026년 1월 1일부터 1월 1일까지 일정 요청
    DB: tstzrange(타임스탬프 범위)로 저장되어 있음.

    2025,01,01 13:00:00: +09:00 처처럼 나노초와 시차(zone)까지 알고 있는 상태

    따라서 변환 과정이 필요함

    DB에게 1월 1일거 전부 내놔라고 하려면
    LocalDate -> OffsetDateTime으로 바꿔야함.


    LocalTime.MAX : 23:59:59:999999999
 */
