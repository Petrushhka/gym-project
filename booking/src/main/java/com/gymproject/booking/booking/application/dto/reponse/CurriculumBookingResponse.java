package com.gymproject.booking.booking.application.dto.reponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
@Schema(description = "커리큘럼 강좌 일괄 예약 응답")
public record CurriculumBookingResponse(
        @Schema(description = "커리큘럼(강좌) 명칭", example = "8주 완성 다이어트 챌린지")
        String curriculumName,

        @Schema(description = "총 수업 횟수", example = "24")
        int totalCounts,

        @Schema(description = "예약 상세 내역 리스트")
        List<BookingDetail> bookingDetails

) {
    /**
     * 개별 수업의 예약 상태를 담는 내부 DTO
     */
    public record BookingDetail(
            @Schema(description = "스케줄 ID", example = "501")
            Long scheduleId,

            @Schema(description = "수업 날짜", example = "2026-02-01")
            LocalDate date,

            @Schema(description = "수업 시작 시간", example = "10:00")
            LocalTime startTime,

            @Schema(description = "예약 처리 결과", example = "SUCCESS")
            String status){ }


    public static CurriculumBookingResponse create(String name, int count, List<BookingDetail> details) {
        return CurriculumBookingResponse.builder()
                .curriculumName(name)
                .totalCounts(count)
                .bookingDetails(details)
                .build();
    }
}
