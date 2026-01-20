package com.gymproject.booking.booking.application.dto.reponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "커리큘럼 강좌 일괄 예약 취소 응답")
public record CurriculumCancelResponse(
        @Schema(description = "커리큘럼 명칭", example = "8주 바디프로필 챌린지")
        String curriculumName,

        @Schema(description = "취소 성공 횟수", example = "15")
        int cancelledCount

) {
}
