package domain.policy;

import com.gymproject.booking.timeoff.domain.policy.TrainerTimeOffPolicy;
import com.gymproject.booking.timeoff.domain.type.TimeOffStatus;
import com.gymproject.booking.timeoff.exception.TimeOffException;
import io.hypersistence.utils.hibernate.type.range.Range;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainerTimeOffPolicyTest {
    private static final OffsetDateTime NOW = OffsetDateTime.now();

    @Test
    @DisplayName("시간 범위 생성 성공: 시작 시간 포함, 종료 시간 미포함 [s, e)")
    void createTimeRange_Success() {
        // given
        OffsetDateTime start = NOW;
        OffsetDateTime end = NOW.plusHours(1);

        // when
        Range<ZonedDateTime> range = TrainerTimeOffPolicy.createTimeRange(start, end);

        // then
        // 1. 범위 타입 확인 (closedOpen)
        assertThat(range.isLowerBoundClosed()).isTrue();
        assertThat(range.isUpperBoundClosed()).isFalse();

        // 2. 시간 변환 확인 (ZonedDateTime으로 잘 변환되었는지)
        assertThat(range.lower().toOffsetDateTime()).isEqualTo(start); // 시간 값 비교
    }

    @Test
    @DisplayName("시간 범위 실패: 종료 시간이 시작 시간보다 앞설 때")
    void createTimeRange_Fail() {
        // given
        OffsetDateTime start = NOW;
        OffsetDateTime end = NOW.minusHours(1); // 과거

        // when & then
        assertThatThrownBy(() -> TrainerTimeOffPolicy.createTimeRange(start, end))
                .isInstanceOf(TimeOffException.class)
                .hasFieldOrPropertyWithValue("errorCode","TIMEOFF_INVALID_TIME_RANGE");
    }

    @Test
    @DisplayName("취소 검증 실패: 이미 취소된 상태")
    void validateCancellation_Fail() {
        // given
        TimeOffStatus status = TimeOffStatus.CANCELLED;

        // when & then
        assertThatThrownBy(() -> TrainerTimeOffPolicy.validateCancellation(status))
                .isInstanceOf(TimeOffException.class)
                .hasFieldOrPropertyWithValue("errorCode", "TIMEOFF_ALREADY_CANCELLED");
    }

    @Test
    @DisplayName("소유자 검증 실패: 요청자가 다를 때")
    void validateOwner_Fail() {
        // given
        Long ownerId = 1L;
        Long requesterId = 2L;

        // when & then
        assertThatThrownBy(() -> TrainerTimeOffPolicy.validateOwner(ownerId, requesterId))
                .isInstanceOf(TimeOffException.class)
                .hasFieldOrPropertyWithValue("errorCode", "TIMEOFF_ACCESS_DENIED");
    }
}
