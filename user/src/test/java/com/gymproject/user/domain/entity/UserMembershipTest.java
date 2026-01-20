package com.gymproject.user.domain.entity;

import com.gymproject.common.policy.RefundDecision;
import com.gymproject.common.vo.Modifier;
import com.gymproject.user.domain.entity.util.DomainEventsTestUtils;
import com.gymproject.user.membership.domain.entity.UserMembership;
import com.gymproject.user.membership.domain.event.MembershipChangedEvent;
import com.gymproject.user.membership.domain.policy.MembershipPolicy;
import com.gymproject.user.membership.domain.policy.MembershipRefundPolicy;
import com.gymproject.user.membership.domain.type.MembershipPlanType;
import com.gymproject.user.membership.domain.type.MembershipStatus;
import com.gymproject.user.membership.exception.UserMembershipException;
import com.gymproject.user.profile.domain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserMembershipTest {

    private final OffsetDateTime fixedNow = OffsetDateTime.parse("2025-12-01T00:00:00+00:00");
    private final Modifier testModifier = Modifier.system();
    private final User mockUser = Mockito.mock(User.class);

    @Test
    @DisplayName("1. 정상적인 정보로 멤버십 생성 시 ACTIVE 상태여야 한다.")
    void create_success() {
        // given
        MembershipPlanType plan = MembershipPlanType.MONTH_1;
        OffsetDateTime startDate = fixedNow.plusDays(1); // 내일부터 멤버십 시작

        // when
        UserMembership membership = UserMembership.create(
                mockUser, plan, startDate, fixedNow, testModifier
        );
        // then
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.getPlanType()).isEqualTo(plan);
        assertThat(membership.getStartedAt()).isEqualTo(startDate);
        assertThat(membership.getExpiredAt()).isEqualTo(startDate.plusMonths(1));
    }

    @Test
    @DisplayName("2. 과거 날짜로 멤버십을 생성하려고 하변 예외가 발생한다.")
    void create_fail_past_date() {
        // given
        OffsetDateTime pastDate = fixedNow.minusDays(1);

        // when & then
        assertThatThrownBy(()-> UserMembership.create(
                mockUser, MembershipPlanType.MONTH_1, pastDate, fixedNow, testModifier
        ))
                .isInstanceOf((UserMembershipException.class))
                .hasMessageContaining("과거 날짜로 멤버십을 생성할 수 없습니다.");
    }


    @Test
    @DisplayName("3. 멤버십 연장 시, 만료일로부터 3개월이 지나면 예외가 발생한다.")
    void extend_fail_over_3_months() {
        // given
        OffsetDateTime pastExpiredAt = fixedNow.minusMonths(4); // 3개월 전이면 실패함
        UserMembership membership = createExpiredMembership(pastExpiredAt);

        // when & then
        assertThatThrownBy(()-> membership.extend(MembershipPlanType.MONTH_3, testModifier, fixedNow))
                .isInstanceOf((UserMembershipException.class))
                .hasMessageContaining("만료 후 연장 가능 기간(3개월)이 지났습니다. 신규가입을 진행해주세요.");
    }

    @Test
    @DisplayName("4. 멤버십 연장 성공 시 만료일이 업데이트 되고 이벤트가 등록된다.")
    void extend_success() {
        // given
        UserMembership membership = createActiveMembership(); // 12월~1월 멤버십 생성
        DomainEventsTestUtils.clearEvents(membership); // 생성 시 발생한 이벤트 제거

        OffsetDateTime firstExpiredAt = membership.getExpiredAt(); // 연장 전 만료일

        // when

        // 1개월 연장
        membership.extend(MembershipPlanType.MONTH_1, testModifier, fixedNow);

        // then
        assertThat(membership.getExpiredAt())
                .isEqualTo(firstExpiredAt.plusMonths(1)); //

        // 이벤트 검증
        List<Object> events = DomainEventsTestUtils.getEvents(membership);
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(MembershipChangedEvent.class);
    }

    @Test
    @DisplayName("5. 만료된 지 3개월 이내라면 연장이 가능해야한다.")
    void extend_success_within_3_months() {
        // given
        // 9월에 멤버십 Expire
        UserMembership membership = createExpiredMembership(fixedNow.minusMonths(3));
        OffsetDateTime originalStartedAt = membership.getStartedAt();
        // when
        // 12월 기준으로 다시 연장 시킴
        membership.extend(MembershipPlanType.MONTH_1, testModifier, fixedNow);

        // then
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(membership.getPlanType()).isEqualTo((MembershipPlanType.MONTH_1));

        // 12월 기준으로 1개월 연장됨
        assertThat(membership.getExpiredAt()).isEqualTo(fixedNow.plusMonths(1));
        // 시작일은 변하지 않음
        assertThat(membership.getStartedAt()).isEqualTo(originalStartedAt);
    }

    @Test
    @DisplayName("6. 90일 이내의 기간은 일시정지가 가능해야 한다.")
    void suspend_success() {
        // given
        UserMembership membership = UserMembership.create(
                mockUser, MembershipPlanType.MONTH_6, fixedNow, fixedNow, testModifier
        );
        OffsetDateTime startDate = fixedNow.plusDays(1);
        OffsetDateTime endDate = fixedNow.plusDays(91); // 맥시멈 90일까지 가능
        // when
        membership.suspend(testModifier, "개인 사정으로 일시정지", startDate, endDate, fixedNow);

        // then
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.SUSPENDED);
        assertThat(membership.getStartedAt()).isEqualTo(fixedNow);
        assertThat(membership.getSuspendStartAt()).isEqualTo(startDate);
    }

    @Test
    @DisplayName("7. 일시정지 해제 시 정지되었던 일수만큼 만료일이 연장되어야 한다.")
    void resume_success() {
        // given
        UserMembership membership = UserMembership.create(
                mockUser, MembershipPlanType.MONTH_6, fixedNow, fixedNow, testModifier
        );
        OffsetDateTime beforeExpiredAt = membership.getExpiredAt();

        OffsetDateTime suspendStrat = fixedNow.minusDays(10);
        membership.suspend(testModifier, "일시정지",
                suspendStrat, fixedNow, fixedNow.minusDays(10));

        // when
        membership.resume(testModifier, fixedNow);
        // then
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat((membership.getExpiredAt())).isEqualTo(beforeExpiredAt.plusDays(10));

    }

    @Test
    @DisplayName("8. 시작 전 멤버십은 전액 환불(취소)이 가능해야 한다.")
    void cancel_success() {
        // given
        OffsetDateTime startDate = fixedNow.plusDays(7);
        UserMembership membership = UserMembership.create(
                mockUser, MembershipPlanType.MONTH_6, startDate, fixedNow, testModifier
        );
        DomainEventsTestUtils.clearEvents(membership);

        // when
        membership.cancel(testModifier, fixedNow);

        // then
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.CANCELLED);
        assertThat((membership.getExpiredAt())).isEqualTo(fixedNow);

        // 이벤트 확인
        List<Object> events = DomainEventsTestUtils.getEvents(membership);
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(MembershipChangedEvent.class);
    }
    
    @Test
    @DisplayName("9. 시작되어 14일이 경과한 멤버십은 환불하려고하면 예외가 발생한다.")
    void rollback_failed() {
        // given
        OffsetDateTime startDate = fixedNow.minusDays(15); // 14일이면 에러남
        UserMembership membership = UserMembership.create(
                mockUser, MembershipPlanType.MONTH_6, startDate, startDate, testModifier
        );

        // when & then
        assertThatThrownBy(()-> membership.rollbackByRefund(testModifier, fixedNow))
                .isInstanceOf((UserMembershipException.class))
                .hasMessageContaining("시작일로부터 14일이 지난 멤버십은 환불 불가합니다.");
    }

    @Test
    @DisplayName("10. 시작된지 14일 이내인 멤버십은 부분환불이 가능해야 한다.")
    void rollback_success() {
        // given
        OffsetDateTime startDate = fixedNow.minusDays(14);
        UserMembership membership = UserMembership.create(
                mockUser, MembershipPlanType.MONTH_6, startDate, startDate, testModifier
        );
        DomainEventsTestUtils.clearEvents(membership);

        // when
        membership.rollbackByRefund(testModifier, fixedNow);

        // then
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.CANCELLED);
        assertThat(membership.getExpiredAt()).isEqualTo(fixedNow);

        // 이벤트 검증
        List<Object> events = DomainEventsTestUtils.getEvents(membership);
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(MembershipChangedEvent.class);
    }


    @Test
    @DisplayName("11. 14만원 결제 후 멤버십 시작 1초라도 지나면 하루를 사용한 것으로 간주한다.(13만원 환불)")
    void refundRate_oneSecondAfterStart() {
        // given
        long paidAmount = 140000L;
        OffsetDateTime startDate = fixedNow;
        UserMembership membership = UserMembership.create(
                mockUser, MembershipPlanType.MONTH_1, startDate, startDate, testModifier
        );
        // when
        OffsetDateTime oneSecondLater = startDate.plusSeconds(1);
        double rate = MembershipPolicy.calculateRefundRate(startDate, oneSecondLater);

        MembershipRefundPolicy refundPolicy = new MembershipRefundPolicy();
        RefundDecision decision = refundPolicy.calculate(membership, oneSecondLater, paidAmount);

        // then
        assertThat(rate).isEqualTo(13.0/14.0);
        assertThat(decision.refundAmount()).isEqualTo(130000L);
    }

    @Test
    @DisplayName("11-1. [버그 검출] 25시간(1일 1시간)이 지났다면, 올림 처리되어 '2일'치를 공제해야 한다.")
    void refundRate_25hours_test() {
        // given
        // 14만원 결제
        long paidAmount = 140000L;
        OffsetDateTime startDate = fixedNow;

        // when: 25시간 후 (하루 하고도 1시간 더 지남)
        OffsetDateTime twentyFiveHoursLater = startDate.plusHours(25);

        MembershipRefundPolicy refundPolicy = new MembershipRefundPolicy();
        UserMembership membership = UserMembership.create(mockUser, MembershipPlanType.MONTH_1, startDate, startDate, testModifier);
        RefundDecision decision = refundPolicy.calculate(membership, twentyFiveHoursLater, paidAmount);

        // then
        // 기대: 14일 중 2일 사용 -> 12일치 환불 (120,000원)
        assertThat(decision.refundAmount()).isEqualTo(120000L);
    }

    @Test
    @DisplayName("12. 이미 취소된 멤버십은 연장할 수 없다.")
    void extend_fail_when_cancelled() {
        // given
        UserMembership membership = createActiveMembership();
        membership.cancel(testModifier, fixedNow); // 취소 상태로 변경

        // when & then
        assertThatThrownBy(()-> membership.extend(MembershipPlanType.MONTH_1, testModifier, fixedNow))
                .isInstanceOf(UserMembershipException.class)
                .hasMessageContaining("현재 멤버십 상태에서는 해당 작업을 수행할 수 없습니다.");
    }

    @Test
    @DisplayName("13. 90일 최대 기간 정지 후 해제 시 만료일이 정확히 90일 늘어나야한다.")
    void suspend_max_days_resume_test() {
// given
        UserMembership membership = UserMembership.create(mockUser, MembershipPlanType.MONTH_6, fixedNow, fixedNow, testModifier);
        OffsetDateTime originalExpiredAt = membership.getExpiredAt();

        // 90일간 정지
        OffsetDateTime start = fixedNow;
        OffsetDateTime end = fixedNow.plusDays(90);
        membership.suspend(testModifier, "최대기간정지", start, end, fixedNow);

        // when: 90일이 지난 시점에 해제
        membership.resume(testModifier, end);

        // then
        assertThat(membership.getExpiredAt()).isEqualTo(originalExpiredAt.plusDays(90));
    }
    // --- 헬퍼 메서드

    private UserMembership createActiveMembership() {
        OffsetDateTime startDate = fixedNow;

        return UserMembership.create(mockUser, MembershipPlanType.MONTH_1,
                startDate, fixedNow, testModifier);
    }

    private UserMembership createExpiredMembership(OffsetDateTime targetExpiredAt) {
        OffsetDateTime startDate = targetExpiredAt.minusMonths(1);

        UserMembership membership = UserMembership.create(mockUser, MembershipPlanType.MONTH_1,
                startDate, startDate, testModifier);

        membership.expire(testModifier, targetExpiredAt);

        return membership;
    }

}