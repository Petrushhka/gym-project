package com.gymproject.booking.api;

import com.gymproject.booking.booking.application.BookingService;
import com.gymproject.booking.booking.application.dto.reponse.BookingHistoryResponse;
import com.gymproject.booking.booking.application.dto.reponse.BookingResponse;
import com.gymproject.booking.booking.application.dto.reponse.CurriculumBookingResponse;
import com.gymproject.booking.booking.application.dto.reponse.CurriculumCancelResponse;
import com.gymproject.booking.booking.application.dto.request.AttendanceRequest;
import com.gymproject.booking.booking.application.dto.request.BookingHistorySearchCondition;
import com.gymproject.booking.booking.application.dto.request.BookingReviewRequest;
import com.gymproject.booking.booking.application.dto.request.PTRequest;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.dto.exception.CommonResDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1. 수업 예약 관리", description = "수업 예약 신청, 승인, 취소 및 출석 관리")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    // 1] 회원 1:1 예약 신청
    @Operation(summary = "1. 1:1 PT 예약 신청(회원)", description = "회원이 원하는 시간대에 트레이너에게 1:1 수업 예약을 요청합니다.")
    @PostMapping
    public ResponseEntity<CommonResDto<BookingResponse>> createBooking(@RequestBody @Valid PTRequest ptRequest,
                                                                       @AuthenticationPrincipal UserAuthInfo userAuthInfo) {
        BookingResponse response = bookingService.createPTBooking(ptRequest, userAuthInfo);

        return ResponseEntity.ok(CommonResDto.success(200, "예약 신청이 완료되었습니다.", response));
    }


    // 2] 트레이너 1:1 (체험신청) 예약 확정 및 거절
    @Operation(summary = "2. 예약 승인 및 거절(트레이너)", description = "트레이너가 회원의 1:1 예약 요청을 검토하고 확정(CONFIRM) 또는 거절(REJECT)합니다.")
    @PostMapping("/{bookingId}/status")
    public ResponseEntity<CommonResDto<BookingResponse>> confirmedBooking(@PathVariable("bookingId") Long bookingId,
                                                                          @RequestBody BookingReviewRequest request, // 여기서 confrim, reject를 보내야함.
                                                                          @AuthenticationPrincipal UserAuthInfo userAuthInfo) {
        BookingResponse response = bookingService.reviewBookingRequest(bookingId, request.getType().name(), userAuthInfo);
        return ResponseEntity.ok(CommonResDto.success(200, "예약 상태가 업데이트되었습니다.", response));
    }

    // 3] PEROSNAL 또는 GROUP_ROUTINE 수업 취소
    @Operation(summary = "3. 예약 취소 (개인/루틴)", description = "확정된 1:1 예약이나 루틴형 그룹 수업 예약을 취소합니다.")
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<CommonResDto<BookingResponse>> cancelBooking(@PathVariable("bookingId") Long bookingId,
                                                                       @AuthenticationPrincipal UserAuthInfo userAuthInfo) {
        BookingResponse response = bookingService.cancelBooking(bookingId, userAuthInfo);
        return ResponseEntity.ok(CommonResDto.success(200, "예약이 취소되었습니다.", response));
    }

    // 4] 수업 출석 체크
    @Operation(
            summary = "4. GPS 기반 수업 출석 체크",
            description = """
                    사용자의 현재 위치(위도, 경도)를 전송하여 수업 출석을 완료합니다.
                    
                    1. 서버는 수업 장소의 좌표와 사용자의 현재 좌표 사이의 거리를 계산하여 설정된 반경(예: 100m) 내에 있을 때만 출석을 인정합니다.
                    2. 환불 관련 요구에 대응하기 위한 기능
                    """
    )
    @PostMapping("/{bookingId}/attendance")
    public ResponseEntity<CommonResDto<BookingResponse>> checkIn(@PathVariable("bookingId") Long bookingId,
                                                                 @RequestBody AttendanceRequest attendanceRequest,
                                                                 @AuthenticationPrincipal UserAuthInfo userAuthInfo) {

        // 서비스에서 bookingId와 좌표를 받아 거리 검증 및 상태 변경을 처리
        BookingResponse response = bookingService.processGpsCheckIn(
                bookingId,
                attendanceRequest.getLatitude(),
                attendanceRequest.getLongitude(),
                userAuthInfo
        );

        return ResponseEntity.ok(CommonResDto.success(200, "출석이 확인되었습니다.", response));
    }

    // 5] 커리큘럼형 수업 참여
    @Operation(summary = "5. 커리큘럼형 수업 예약", description = "예) 8주 다이어트 프로그램 등 전체 커리큘럼 단위의 강좌에 일괄 예약합니다.")
    @PostMapping("/curriculums/{recurrenceId}")
    public ResponseEntity<CommonResDto<CurriculumBookingResponse>> enterRecurrence(@PathVariable("recurrenceId") Long recurrenceId,
                                                                                   @AuthenticationPrincipal UserAuthInfo userAuthInfo) {

        CurriculumBookingResponse response = bookingService.reserveCurriculum(recurrenceId, userAuthInfo);
        return ResponseEntity.ok(CommonResDto.success(200, "강좌 참여가 완료되었습니다.", response));
    }

    // 6] 루틴형 수업 참여(Schedule 단위로 예약해야함)
    @Operation(summary = "6. 루틴형 수업 개별 참여", description = "매주 동일하게 진행되는 루틴형 수업 중 특정 날짜의 수업을 예약합니다.")
    @PostMapping("/schedules/{scheduleId}")
    public ResponseEntity<CommonResDto<BookingResponse>> enterDayClass(@PathVariable("scheduleId") Long scheduleId,
                                                                       @AuthenticationPrincipal UserAuthInfo userAuthInfo) {
        BookingResponse response = bookingService.enterRoutineClass(scheduleId, userAuthInfo);

        return ResponseEntity.ok(CommonResDto.success(200, "수업 예약이 완료되었습니다.", response));
    }

    // 7] 커리큘럼형 수업 취소
    @Operation(
            summary = "7. 커리큘럼형 강좌 전체 취소",
            description = """
                    특정 정기 강좌(Curriculum)에 대한 모든 예약을 일괄 취소합니다.
                    
                    1. 해당 커리큘럼으로 생성된 모든 미래의 수업 스케줄에서 사용자의 예약 데이터가 제거됩니다.
                    """
    )
    @DeleteMapping("/curriculums/{recurrenceId}")
    public ResponseEntity<CommonResDto<CurriculumCancelResponse>> cancelCurriculum(@PathVariable("recurrenceId") Long recurrenceId,
                                                                                   @AuthenticationPrincipal UserAuthInfo userAuthInfo) {
        CurriculumCancelResponse response = bookingService.cancelCurriculumBooking(recurrenceId, userAuthInfo);

        return ResponseEntity.ok(
                CommonResDto.success(200, "전체 강좌 예약이 성공적으로 취소되었습니다.", response)
        );
    }

    @Operation(
            summary = "예약 변경 이력 검색",
            description = """
                    다양한 조건으로 예약 변경 이력을 조회합니다. (페이징 지원)
                    
                    - bookingId: 특정 예약의 이력만 조회
                    - modifierId: 특정 관리자나 회원이 변경한 내역 조회
                    - actionType: 취소(CANCEL), 노쇼(NOSHOW) 등 특정 이벤트 필터링
                    - 기간 검색: startDate ~ endDate (호주 시간 기준)
                    """
    )
    @GetMapping
    public ResponseEntity<CommonResDto<Page<BookingHistoryResponse>>> searchBookingHistories(
            // @ParameterObject: Swagger에서 쿼리 파라미터를 펼쳐서 보여줌
            @ParameterObject @ModelAttribute BookingHistorySearchCondition condition,

            // 기본 정렬: 최신순 (내림차순)
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,

            @AuthenticationPrincipal UserAuthInfo userAuthInfo
    ) {

        // 회원들은 본인의 기록만 보게 해야함(덮어주는 작업)
        if (!userAuthInfo.isOverTrainer()) {
            condition = new BookingHistorySearchCondition(
                    userAuthInfo.getUserId(),
                    condition.modifierId(),
                    condition.actionType(),
                    condition.startDate(),
                    condition.endDate(),
                    condition.modifierRole(),
                    condition.bookingType(),
                    condition.status()
            );
        }


        Page<BookingHistoryResponse> response = bookingService.searchHistories(condition, pageable);

        return ResponseEntity.ok(CommonResDto.success(200, "예약 이력 조회가 완료되었습니다.", response));
    }

//    @Operation(summary = "수업 다중 예약 (벌크)", description = "날짜 또는 수업 종류별 선택한 여러 개의 수업을 한 번에 예약합니다.")
//    @PostMapping("/bulk")
//    public ResponseEntity<CommonResDto<BulkBookingResponse>> bulkBooking(
//            @RequestBody BulkBookingRequest request,
//            @AuthenticationPrincipal UserAuthInfo userAuthInfo) {
//
//        // 서비스에서 일괄 처리 로직 수행
//        BulkBookingResponse response = bookingService.bookMultipleSchedules(request, userAuthInfo);
//
//        return ResponseEntity.ok(CommonResDto.success(201, "선택한 수업들이 예약되었습니다.", response));
//    }


}
