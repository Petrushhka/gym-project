package com.gymproject.booking.api;

import com.gymproject.booking.booking.application.dto.reponse.TimeOffResponse;
import com.gymproject.booking.timeoff.application.TrainerTimeOffService;
import com.gymproject.booking.timeoff.application.dto.TimeOffRequest;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.exception.CommonResDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "2. 예약 막기", description = "트레이너 개인 일정 차단 및 시간 관리")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/time-offs")
public class TimeOffController {

    private final TrainerTimeOffService trainerTimeOffService;

    // 트레이너 예약금지 시간등록
    @Operation(
            summary = "1. 트레이너 예약 금지 시간 등록",
            description = """
            트레이너의 개인 일정(휴가, 식사 시간 등)을 위해 특정 시간대를 예약 불가 상태로 설정합니다.
            
            1. 해당 시간대에 생성된 빈 스케줄이 있다면 '차단' 상태로 변경되어 회원이 예약할 수 없게 됩니다.
            2. 시작 시간과 종료 시간을 포함한 `TimeOffRequest`를 전달받습니다.
            """
    )
    @PostMapping
    public ResponseEntity<CommonResDto<TimeOffResponse>> blockBooking(@RequestBody @Valid TimeOffRequest timeOffRequest,
                                                                      @AuthenticationPrincipal UserAuthInfo userAuthInfo){

      TimeOffResponse response =  trainerTimeOffService.blockSchedule(timeOffRequest, userAuthInfo);

        return ResponseEntity.ok(
                CommonResDto.success(200, "예약 금지 시간이 설정되었습니다.", response)
        );    }

    // 트레이너 예약금지 시간 풀기
    @Operation(
            summary = "2. 예약 금지 시간 해제",
            description = "설정했던 예약 금지(Block)를 해제하여 다시 해당 시간에 예약을 받을 수 있도록 변경합니다."
    )
    @DeleteMapping("/{blockId}")
    public ResponseEntity<CommonResDto<TimeOffResponse>> cancelBlockTime(@PathVariable("blockId") Long blockId,
                                                           @AuthenticationPrincipal UserAuthInfo userAuthInfo){

       TimeOffResponse response = trainerTimeOffService.cancelBlockTime(blockId, userAuthInfo);

        return ResponseEntity.ok(
                CommonResDto.success(200, "예약 금지 설정이 해제되었습니다.", response)
        );
    }

}
