package com.gymproject.user.domain.entity;

import com.gymproject.common.vo.Modifier;
import com.gymproject.user.domain.entity.util.DomainEventsTestUtils;
import com.gymproject.user.profile.domain.entity.User;
import com.gymproject.user.profile.domain.type.UserSessionStatus;
import com.gymproject.user.sesssion.domain.entity.UserSession;
import com.gymproject.user.sesssion.domain.event.SessionChangedEvent;
import com.gymproject.user.sesssion.domain.type.SessionProductType;
import com.gymproject.user.sesssion.exception.UserSessionsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserSessionTest {

    private final OffsetDateTime startDate = OffsetDateTime.parse("2025-12-01T10:00:00Z");
    private final OffsetDateTime endDate = startDate.plusMonths(3);
    private final Modifier testModifier = Modifier.system();
    private final User mockUser = Mockito.mock(User.class);

    @Test
    @DisplayName("1. 세션 사용 시 사용 횟수가 증가하고 이벤트가 등록된다.")
    void use_success() {
        // given
        UserSession userSession =  UserSession.createPaid(
                mockUser,
                SessionProductType.PT_10,
                testModifier,
                startDate,
                endDate);
        DomainEventsTestUtils.clearEvents(userSession); // 이벤트 비우기 (구매 이벤트는 지움)

        // when
        userSession.use(testModifier, startDate);

        // then
        assertThat(userSession.getUsedSessions()).isEqualTo(1);
        assertThat(userSession.getRemainingSessions()).isEqualTo(9);

        List<Object> events = DomainEventsTestUtils.getEvents(userSession);
        assertThat(events).hasSize(1); // use 이벤트 1개만 남음
        assertThat(events.get(0)).isInstanceOf(SessionChangedEvent.class); // use된 이벤트의 종류는 SessionChangedEvent임

    }

    @Test
    @DisplayName("2. 마지막 세션을 사용하면 상태가 FULLY_USED로 변경된다.")
    void use_becomes_fully_used() {
        // given
        UserSession userSession = UserSession.createFreeTrial(mockUser, testModifier, startDate);

        // when
        userSession.use(testModifier, startDate);

        // then
        assertThat(userSession.getUsedSessions()).isEqualTo(1);
        assertThat(userSession.getRemainingSessions()).isEqualTo(0);
        assertThat(userSession.getStatus()).isEqualTo(UserSessionStatus.FULLY_USED);
    }

    @Test
    @DisplayName("3. 만료일이 지난 세션을 사용하려고 하면 예외가 발생한다.")
    void use_fail_expired() {
        // given
        UserSession userSession = UserSession.createPaid(mockUser, SessionProductType.PT_10, testModifier, startDate, startDate.minusDays(1));

        // when & then
        assertThatThrownBy(() -> userSession.use(testModifier, startDate))
                .isInstanceOf(UserSessionsException.class)
                .hasMessageContaining("만료된 세션권입니다.");
        }

        @Test
        @DisplayName("4. FUllY_USED 상태에서 복구하면 ACTIVE 상태로 돌아온다.")
        void restore_success() {
            // given
            UserSession userSession = UserSession.createFreeTrial(mockUser, testModifier, startDate);
            userSession.use(testModifier, startDate);
            assertThat(userSession.getStatus()).isEqualTo(UserSessionStatus.FULLY_USED);

            // when
            userSession.restore(testModifier);

            // then
            assertThat(userSession.getUsedSessions()).isEqualTo(0);
            assertThat(userSession.getStatus()).isEqualTo(UserSessionStatus.ACTIVE);

    }

    @Test
    @DisplayName("5. 사용 횟수가 0인데 복구(취소)하려고하면 예외가 발생한다.")
    void restore_fail_when_unused() {
        // given
        UserSession userSession = UserSession.createPaid(mockUser,SessionProductType.PT_10, testModifier, startDate, endDate);
        // when & then
        assertThatThrownBy(() -> userSession.restore(testModifier))
        .isInstanceOf(UserSessionsException.class)
                .hasMessageContaining("복구할 수 없는 상태이거나 내역이 없습니다.");
    }

    @Test
    @DisplayName("6. 무료 체험권은 발급 후 30일이 지나면 사용할 수 없다.")
    void free_trial_after_30_days() {
        // given
        UserSession userSession = UserSession.createFreeTrial(mockUser, testModifier, startDate);
        // when
        OffsetDateTime usedAt = startDate.plusDays(31);
        // then
        assertThatThrownBy(()-> userSession.use(testModifier, usedAt))
                .isInstanceOf(UserSessionsException.class)
                .hasMessageContaining("만료된 세션권입니다.");
    }

    @Test
    @DisplayName("7. 무료 체험권은 발급 후 29일째에는 정상적으로 사용 가능하다.")
    void free_trial_can_be_used_within_30_days() {
        // given
        UserSession userSession = UserSession.createFreeTrial(mockUser, testModifier, startDate);
        // when
        OffsetDateTime usedAt = startDate.plusDays(29);
        userSession.use(testModifier, usedAt);

        // then
        assertThat(userSession.getStatus()).isEqualTo(UserSessionStatus.FULLY_USED);
        assertThat(userSession.getUsedSessions()).isEqualTo(1);
    }

    @Test
    @DisplayName("8. 한 번도 사용하지 않은 세션은 전액 환불 처리가 가능하다.")
    void refund_success() {
        // given
            UserSession userSession = UserSession.createPaid(mockUser, SessionProductType.PT_10, testModifier, startDate, endDate);
            DomainEventsTestUtils.clearEvents(userSession);

        // when
        userSession.refund(testModifier);

        // then
        assertThat(userSession.getStatus()).isEqualTo(UserSessionStatus.REFUNDED);

        // 이벤트 확인
        List<Object> events = DomainEventsTestUtils.getEvents(userSession);
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(SessionChangedEvent.class);
    }

    @Test
    @DisplayName("9. 1회라도 사용한 세션은 환불할 수 없다.")
    void refund_fail_after_use() {
        // given
        UserSession userSession = UserSession.createPaid(mockUser, SessionProductType.PT_10, testModifier, startDate, endDate);
        userSession.use(testModifier, startDate);

        // when & then
        assertThatThrownBy(() -> userSession.refund(testModifier))
                .isInstanceOf(UserSessionsException.class)
                .hasMessageContaining("이미 사용된 세션권 입니다.");
    }

    @Test
    @DisplayName("10. 이미 환불된 세션은 복구(restore) 할 수 없다.")
    void restore_fail_when_refunded() {
        // given
        UserSession userSession = UserSession.createPaid(mockUser,SessionProductType.PT_10, testModifier, startDate, endDate);
        userSession.refund(testModifier);

        // when & then
        assertThatThrownBy(() -> userSession.restore(testModifier))
                .isInstanceOf(UserSessionsException.class)
                .hasMessageContaining("복구할 수 없는 상태이거나 내역이 없습니다.");
    }
}