package com.gymproject.booking.timeoff.domain.policy;

import com.gymproject.booking.timeoff.domain.type.TimeOffStatus;
import com.gymproject.booking.timeoff.exception.TimeOffErrorCode;
import com.gymproject.booking.timeoff.exception.TimeOffException;
import io.hypersistence.utils.hibernate.type.range.Range;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static com.gymproject.common.constant.GymTimePolicy.SERVICE_ZONE;

public class TrainerTimeOffPolicy {

    /*
        입력받은 시간을 시스템의 기준으로 ZonedDateTime으로 변환
       시작시간(포함) ~ 종료시간(미포함) 으로 Range 생성([s, e))

             OffsetDateTime -> ZonedDateTime 변환
            시스템의 기본 ZonedId를 사용, 특정 Zone(예: Asia/Seoul)을 지정해야함.

           // 타입에 맞춰 시간 변환
            나의 경우는 종료시간부터는 예약가능하게 Range를 묶어놓음.

            Range.closed[s,e] 처음 제외, 끝 포함
            Range.open(s,e) 처음 제외, 끝 제외
            Range.openClosed(s,e] 처음 제외, 끝 포함.
            Range.closedOpen[s,e) 처음 포함, 끝 제외.

            tstzrange 의 경우에는 [Start, End) : 시작포함, 끝 제외가 기본임

     */
    public static Range<ZonedDateTime> createTimeRange(OffsetDateTime start, OffsetDateTime end) {
        if (!start.isBefore(end)) {
            throw new TimeOffException(TimeOffErrorCode.INVALID_TIME_RANGE);
        }

        ZonedDateTime zonedStart = start.atZoneSameInstant(SERVICE_ZONE);
        ZonedDateTime zonedEnd = end.atZoneSameInstant(SERVICE_ZONE);

        // Postgre tstzrange 기본 포맷인 '[)' (시작 포함, 종료 미포함) 사용
        return Range.closedOpen(zonedStart, zonedEnd);
    }

    // 2. 이미 취소된 건인지 확인
    public static void validateCancellation(TimeOffStatus status) {
        if (status == TimeOffStatus.CANCELLED) {
            throw new TimeOffException(TimeOffErrorCode.ALREADY_CANCELLED);
        }
    }

    // 3. 본인의 휴무인지 확인
    public static void validateOwner(Long ownerId, Long requesterId) {
        if (!ownerId.equals(requesterId)) {
            throw new TimeOffException(TimeOffErrorCode.ACCESS_DENIED);
        }
    }


}
