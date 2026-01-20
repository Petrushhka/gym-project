package domain.policy;

import com.gymproject.booking.booking.application.dto.request.TicketType;
import com.gymproject.booking.booking.domain.policy.BookingPolicy;
import com.gymproject.booking.booking.domain.type.BookingStatus;
import com.gymproject.booking.booking.domain.type.CancellationType;
import com.gymproject.booking.booking.exception.BookingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingPolicyTest {
    // 테스트 기준 시간 (2025-01-01 12:00:00)
    private static final OffsetDateTime NOW = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    @DisplayName("1. 초기 상태 결정: 무료 체험권은 PENDING, 유료 이용권은 CONFIRMED 상태를 반환한다.")    void determineInitialStatus() {
        assertThat(BookingPolicy.determineInitialStatus(TicketType.FREE_TRIAL)).isEqualTo(BookingStatus.PENDING);
        assertThat(BookingPolicy.determineInitialStatus(TicketType.PAID)).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("2. 마감 시간 검증: 마감 시간 이전이면 통과한다.")
    void validateBookingDeadline_Success() {
        // 수업 시작 1시간 1분 전 (예약 가능)
        OffsetDateTime classStartAt = NOW.plusHours(1).plusMinutes(1);
        TicketType ticketType = TicketType.PAID; // deadline 1시간

        // when & then (예외 발생 안 함)
        BookingPolicy.validateBookingDeadline(ticketType, classStartAt, NOW);
    }

    @Test
    @DisplayName("3. 마감 시간 검증: 마감 시간이 지나면 예외가 발생한다.")
    void validateBookingDeadline_Fail() {
        // given: 유료 이용권 (마감 1시간 전)
        TicketType ticketType = TicketType.PAID; // deadline 1시간
        // 수업 시작 59분 전 (예약 불가)
        OffsetDateTime classStartAt = NOW.plusMinutes(59);

        // when & then
        assertThatThrownBy(() -> BookingPolicy.validateBookingDeadline(ticketType, classStartAt, NOW))
                .isInstanceOf(BookingException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BOOKING_DEADLINE_EXCEEDED");
    }

    @Test
    @DisplayName("4. 취소 정책: 예약 후 10분 이내면 무조건 무료 취소")
    void calculateCancellationType_TenMinuteRule() {
        // given
        OffsetDateTime bookedAt = NOW.minusMinutes(9); // 9분 전에 예약함
        OffsetDateTime classStartAt = NOW.plusMinutes(30); // 수업은 곧 시작하지만

        // when
        // 10분 이내라서 무료 취소여야 함
        CancellationType type = BookingPolicy.calculateCancellationType(bookedAt, NOW, classStartAt);

        // then
        assertThat(type).isEqualTo(CancellationType.FREE_CANCEL);
    }

    @Test
    @DisplayName("5. 취소 정책: 수업 24시간 전이면 무료 취소")
    void calculateCancellationType_24HoursBefore() {
        // given
        OffsetDateTime bookedAt = NOW.minusDays(5); // 옛날에 예약함
        OffsetDateTime classStartAt = NOW.plusHours(25); // 수업까지 25시간 남음
        // when
        CancellationType type = BookingPolicy.calculateCancellationType(bookedAt, NOW, classStartAt);
        // then
        assertThat(type).isEqualTo(CancellationType.FREE_CANCEL);
    }

    @Test
    @DisplayName("6. 취소 정책: 수업 1시간 ~ 24시간 사이면 페널티 취소")
    void calculateCancellationType_Penalty() {
        // given
        OffsetDateTime bookedAt = NOW.minusDays(1);
        OffsetDateTime classStartAt = NOW.plusHours(2); // 수업까지 2시간 남음

        // when
        CancellationType type = BookingPolicy.calculateCancellationType(bookedAt, NOW, classStartAt);
        // then
        assertThat(type).isEqualTo(CancellationType.PENALTY_CANCEL);
    }

    @Test
    @DisplayName("7. 취소 정책: 수업 1시간 이내면 취소 불가")
    void calculateCancellationType_Impossible() {
        // given
        OffsetDateTime bookedAt = NOW.minusDays(1);
        OffsetDateTime classStartAt = NOW.plusMinutes(59); // 수업까지 59분 남음
        // when
        CancellationType type = BookingPolicy.calculateCancellationType(bookedAt, NOW, classStartAt);
        // then
        assertThat(type).isEqualTo(CancellationType.IMPOSSIBLE);
    }

    @Test
    @DisplayName("8. 출석 거리 검증: 150m 이내면 성공")
    void validateCheckInDistance() {
        // given
        BookingPolicy.validateCheckInDistance(149.9); // 성공

        // when & then
        assertThatThrownBy(() -> BookingPolicy.validateCheckInDistance(150.1)) // 실패
                .isInstanceOf(BookingException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BOOKING_CHECK_IN_DISTANCE_EXCEEDED");
    }

}