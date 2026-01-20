package com.gymproject.classmanagement.api;

import com.gymproject.classmanagement.recurrence.application.RecurrenceService;
import com.gymproject.classmanagement.recurrence.application.dto.RecurrenceClassClassRequest;
import com.gymproject.classmanagement.recurrence.application.dto.OneTimeClassResponse;
import com.gymproject.classmanagement.recurrence.application.dto.RecurrenceResponse;
import com.gymproject.classmanagement.recurrence.application.dto.OneTimeClassRequest;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.exception.CommonResDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "2. 정기강좌 개설 및 폐강", description = "정기 강좌(커리큘럼, 루틴형) 수업 개설 관리")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/classes")
public class RecurrenceController {

    private final RecurrenceService recurrenceService;

    /**
     * [중요] 수업은 두종류로 나뉨!!!
     * 1. One-Time Group Class: status에 따라서 예약이 가능
     * 2. Recurring Group Class: 과정형 프로글매이면 중간 합류 불가
     * -> 허나 매주 동일한 루틴의 수업의 경우에는 중간에 참여해도 괜찮음.
     * -> 그러나 커리큘럼 기반인 경우에는 중간 참여하면 안됨.
     * <p>
     * e.g) 8주 다이어트 그룹 (커리큘럼형) / 스쿼트 자세 알아가기(동일루틴형)
     */
    @Operation(
            summary = "1. 정기 강좌 개설",
            description = """
                    특정 기간, 요일에 반복되는 정규 수업을 개설합니다.
                    
                    1. 커리큘럼형 수업: 내용의 순서가 있는 수업으로  중간 합류가 불가능합니다.
                    2. 루틴형 수업: 매번 동일한 루틴으로 진행되어 언제든 참여 가능합니다.
                    
                    설정된 반복 규칙(RRULE)에 따라 실제 수업 스케줄(Schedules)이 일괄 생성됩니다.
                    """
    )
    @PostMapping("/recurring")
    public ResponseEntity<CommonResDto<RecurrenceResponse>> openRecurrenceClass(
            @RequestBody @Valid RecurrenceClassClassRequest requestDto,
            @AuthenticationPrincipal UserAuthInfo userAuthInfo) {

        RecurrenceResponse response = recurrenceService.openRecurrenceClass(requestDto, userAuthInfo);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonResDto.success(
                        HttpStatus.CREATED.value(),
                        "정규 강좌가 성공적으로 개설되었습니다.",
                        response
                )
        );
    }

    // 루틴형 수업 등록 (루틴형이지만 하루만 진행되는 수업)
    @Operation(
            summary = "2. 일회성 수업 개설",
            description = """ 
                    "반복되지 않는 단발성 그룹 수업을 개설합니다. 특정 날짜와 시간에 한 번만 진행되는 수업에 사용합니다. """
    )
    @PostMapping("/one-time")
    public ResponseEntity<CommonResDto<OneTimeClassResponse>> openOneTimeClass(
            @RequestBody @Valid OneTimeClassRequest request,
            @AuthenticationPrincipal UserAuthInfo userAuthInfo) {

        OneTimeClassResponse response =
                recurrenceService.openOneTimeGroupClass(request, userAuthInfo);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonResDto.success(
                        HttpStatus.CREATED.value(),
                        "원데이 클래스가 성공적으로 개설되었습니다.",
                        response
                )
        );
    }

    // 그룹 수업 폐강
    @Operation(
            summary = "3. 정기 강좌 폐강",
            description = "개설된 정기 강좌를 폐강 처리합니다. 연결된 모든 미래의 수업 스케줄이 함께 취소됩니다."
    )
    @DeleteMapping("/recurrence/{recurrenceId}")
    public ResponseEntity<CommonResDto<RecurrenceResponse>> cancelRecurrenceClass(
            @PathVariable("recurrenceId") Long recurrenceId,
            @AuthenticationPrincipal UserAuthInfo userAuthInfo) {

        recurrenceService.cancelRecurrenceClass(recurrenceId, userAuthInfo);

        return ResponseEntity.ok(CommonResDto.success(
                HttpStatus.OK.value(),
                "해당 강좌가 폐강 처리되었습니다.",
                null
        ));
    }

}
