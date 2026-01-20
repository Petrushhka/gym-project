package domain;

import com.gymproject.booking.booking.application.dto.request.TicketType;
import com.gymproject.booking.booking.domain.entity.Booking;
import com.gymproject.booking.booking.domain.event.BookingChangedEvent;
import com.gymproject.booking.booking.domain.type.BookingActionType;
import com.gymproject.booking.booking.domain.type.BookingStatus;
import com.gymproject.booking.booking.domain.type.BookingType;
import com.gymproject.booking.booking.domain.type.CancellationType;
import com.gymproject.booking.booking.exception.BookingException;
import com.gymproject.common.vo.Modifier;
import domain.util.DomainEventsTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {
    private static final Long SCHEDULE_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final Long SESSION_ID = 50L;
    private static final Modifier MODIFIER = Modifier.user(USER_ID, "test_user");

    // 현재 시간 고정 (테스트 일관성)
    private static final OffsetDateTime NOW = OffsetDateTime.now();
    private static final OffsetDateTime CLASS_START_AT = NOW.plusDays(3); // 3일 뒤 수업

    @Nested
    @DisplayName("예약 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("1. 유료 티켓으로 생성 시 CONFIRMED 상태 및 CREATED 이벤트 발생")
        void createPersonal_Paid() {
            // when
            Booking booking = Booking.createPersonalBooking(
                    SCHEDULE_ID, USER_ID, SESSION_ID, TicketType.PAID, MODIFIER, CLASS_START_AT, NOW
            );

            // then
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(booking.getBookingType()).isEqualTo(BookingType.PERSONAL);

            // 이벤트 검증
            List<Object> events = DomainEventsTestUtils.getEvents(booking);
            assertThat(events).hasSize(1);
            BookingChangedEvent bookingChangedEvent = (BookingChangedEvent) events.get(0);

            assertThat(bookingChangedEvent.getActionType()).isEqualTo(BookingActionType.CREATE);

            assertThat(bookingChangedEvent.getNewStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("2. 무료 체험권으로 생성 시 PENDING 상태")
        void createPersonal_Free() {
            // when
            Booking booking = Booking.createPersonalBooking(
                    SCHEDULE_ID, USER_ID, SESSION_ID, TicketType.FREE_TRIAL, MODIFIER, CLASS_START_AT, NOW
            );

            // then
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        }

        @Test
        @DisplayName("3. 그룹 수업은 세션ID 없이 생성되며 항상 CONFIRMED")
        void createGroup() {
            // when
            Booking booking = Booking.createGroup(
                    SCHEDULE_ID, USER_ID, BookingType.GROUP_CURRICULUM, MODIFIER, NOW
            );

            // then
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(booking.getUserSessionId()).isNull();
            assertThat(booking.getBookingType()).isEqualTo(BookingType.GROUP_CURRICULUM);
        }
    }

    @Nested
    @DisplayName("예약 취소 테스트")
    class CancelTest {

        @Test
        @DisplayName("4. 24시간 전 취소 시 FREE_CANCEL 타입으로 이벤트 발행")
        void cancel_Free() {
            // given
            Booking booking = createConfirmedBooking();
            OffsetDateTime cancelTime = NOW; // 현재 (수업 3일전)
            DomainEventsTestUtils.clearEvents(booking);

            // when
            booking.cancel(MODIFIER, cancelTime, CLASS_START_AT, "단순 취소");

            // then
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

            // 이벤트 검증 (0번은 생성 이벤트, 1번이 취소 이벤트)
            List<Object> events = DomainEventsTestUtils.getEvents(booking);
            assertThat(events).hasSize(1);

            BookingChangedEvent event = (BookingChangedEvent) events.get(0);
            assertThat(event.getActionType()).isEqualTo(BookingActionType.CANCEL);
            assertThat(event.getNewStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(event.getCancellationType()).isEqualTo(CancellationType.FREE_CANCEL);
        }

        @Test
        @DisplayName("5. 취소 불가능한 시간(1시간 이내)에 취소 시 예외 발생")
        void cancel_Impossible() {
            // given
            // 수업은 3일 뒤에 시작
            Booking booking = createConfirmedBooking();

            // 실수로 예약한 것은 예약 취소가 가능하니까 조작해야함.
            // 예약을 '어제' 한 것으로 시간을 조작합니다. (Grace Period 회피)
            ReflectionTestUtils.setField(booking, "createdAt", NOW.minusDays(1));

            // 수업 시작 50분 전 (NOW + 2일 23시간 10분... 계산이 복잡하니 classStartAt 기준 역산 추천)
            OffsetDateTime nearClassTime = CLASS_START_AT.minusMinutes(50);

            // when & then
            // 수업 시작 50분 전 시점(nearClassTime)에 취소 요청을 보냄.
            assertThatThrownBy(() -> booking.cancel(MODIFIER, nearClassTime, CLASS_START_AT, "급한 취소"))
                    .isInstanceOf(BookingException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "BOOKING_CANCELLATION_NOT_ALLOWED");
        }

        @Test
        @DisplayName("6. 이미 취소된 예약을 다시 취소하면 예외 발생")
        void cancel_AlreadyCancelled() {
            // given
            Booking booking = createConfirmedBooking();
            booking.cancel(MODIFIER, NOW, CLASS_START_AT, "1차 취소");

            // when & then
            assertThatThrownBy(() -> booking.cancel(MODIFIER, NOW, CLASS_START_AT, "2차 취소"))
                    .isInstanceOf(BookingException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "BOOKING_ALREADY_CANCELLED");
        }
    }

    @Nested
    @DisplayName("상태 변경 테스트")
    class StateTest {

        @Test
        @DisplayName("7. 승인(Confirm) 테스트: PENDING -> CONFIRMED")
        void confirm() {
            // given
            Booking booking = Booking.createPersonalBooking(
                    SCHEDULE_ID, USER_ID, SESSION_ID, TicketType.FREE_TRIAL, MODIFIER, CLASS_START_AT, NOW
            ); // PENDING

            // when
            booking.confirm(MODIFIER);

            // then
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
//            BookingChangedEvent event = (BookingChangedEvent) booking.domainEvents().get(1);
//            assertThat(event.getActionType()).isEqualTo(BookingActionType.APPROVE);
        }

        @Test
        @DisplayName("8. 출석(Attend) 테스트: 거리와 시간이 맞으면 성공")
        void attend_Success() {
            // given
            Booking booking = createConfirmedBooking();
            OffsetDateTime checkInTime = CLASS_START_AT.minusMinutes(10); // 수업 10분전
            double validDistance = 50.0;

            // when
            booking.attend(MODIFIER, CLASS_START_AT, checkInTime, validDistance);

            // then
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.ATTENDED);
        }

        @Test
        @DisplayName("9. 출석 실패: 너무 일찍 체크인")
        void attend_Fail_TooEarly() {
            // given
            Booking booking = createConfirmedBooking();
            OffsetDateTime checkInTime = CLASS_START_AT.minusMinutes(30); // 수업 30분전 (15분 전부터 가능)

            // when & then
            assertThatThrownBy(() -> booking.attend(MODIFIER, CLASS_START_AT, checkInTime, 10.0))
                    .isInstanceOf(BookingException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "BOOKING_CHECK_IN_TOO_EARLY");
        }
    }

    // 테스트 헬퍼 메서드
    private Booking createConfirmedBooking() {
        return Booking.createPersonalBooking(
                SCHEDULE_ID, USER_ID, SESSION_ID, TicketType.PAID, MODIFIER, CLASS_START_AT, NOW
        );
    }



}