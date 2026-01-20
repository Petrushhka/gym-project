package com.gymproject.classmanagement.api;

import com.gymproject.classmanagement.schedule.application.ScheduleService;
import com.gymproject.classmanagement.schedule.application.dto.ScheduleResponse;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.exception.CommonResDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
@Tag(name = "3. 개별 수업 관리", description = "개별 수업 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 1. 달력 스케줄 조회(월별/ 주별)
    @Operation(
            summary = "1. 일정 목록 조회",
            description = """
            특정 기간 내의 트레이너 일정을 조회합니다. 
            
            1. 날짜 범위: 프론트엔드 달력 뷰(월/주/일)에 따라 `startDate`와 `endDate`를 전달받아 해당 범위의 데이터만 효율적으로 조회합니다.
            2. 타임존: 호주 브리즈번(Australia/Brisbane) 시간대가 적용된 `OffsetDateTime` 결과가 반환됩니다.
            """
    )
    @GetMapping
    public ResponseEntity<CommonResDto<List<ScheduleResponse>>> getSchedules(
            @RequestParam("trainerId") Long trainerId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
            ) {

        /*
           [날짜 처리 Tip]
           프론트엔드에서 달력을 넘길 때마다 해당 월의 1일~말일 (또는 앞뒤 여유분 포함)을
           startDate, endDate로 보내주게 하는 것이 서버 부하 관리에 가장 좋습니다.
           (서버에서 무조건 +-3개월을 하드코딩하는 것보다 유연함)
        */
        List<ScheduleResponse> responses = scheduleService.getMonthlySchedules(trainerId, startDate, endDate);

        return ResponseEntity.ok(CommonResDto.success(
                HttpStatus.OK.value(),
                "스케줄 조회 성공",
                responses
        ));
    }

    // 2. 스케줄 강제 폐강
    @Operation(
            summary = "2. 개별 수업 폐강",
            description = "반복 강좌 전체가 아닌, 특정 날짜의 수업 하나를 개별적으로 취소 처리합니다."
    )
    @DeleteMapping("/{scheduleId}") // pathVariable 옆에 명시안하면 에러남
    public ResponseEntity<CommonResDto<ScheduleResponse>> deleteSchedule(@PathVariable("scheduleId") Long scheduleId,
                                                          @AuthenticationPrincipal UserAuthInfo userAuthInfo) {

      ScheduleResponse response = scheduleService.cancelSchedule(scheduleId, userAuthInfo);

        return ResponseEntity.ok(CommonResDto.success(
                HttpStatus.OK.value(),
                "수업이 폐강 처리되었습니다.",
                response
        ));
    }

}
