package com.gymproject.auth.domain.entity;

import com.gymproject.auth.domain.event.IdentityCreatedEvent;
import com.gymproject.auth.exception.IdentityErrorCode;
import com.gymproject.auth.exception.IdentityException;
import com.gymproject.auth.util.DomainEventsTestUtils;
import com.gymproject.common.event.domain.ProfileInfo;
import com.gymproject.common.security.AuthProvider;
import com.gymproject.common.security.Roles;
import com.gymproject.common.security.SexType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityTest {
    // 테스트용 더미 프로필 정보
    private final ProfileInfo TEST_PROFILE = new ProfileInfo(
            "Gildong", "Hong", "0412 345 678", SexType.MALE
    );

    @Test
    @DisplayName("일반 회원가입 성공 시 GUEST 권한을 갖고, 가입 이벤트가 발행되어야 한다.")
    void signUp_success() {
        // given
        String email = "test@test.com";
        String password = "EncodedPassword123!";

        // when
        Identity identity = Identity.signUp(email, password, TEST_PROFILE);

        // then
        assertThat(identity.getEmail()).isEqualTo(email);
        assertThat(identity.getPassword()).isEqualTo(password);
        assertThat(identity.getRole()).isEqualTo(Roles.GUEST); // 초기 상태
        assertThat(identity.isUnsubscribe()).isFalse();

        List<Object> events = DomainEventsTestUtils.getEvents(identity);

        // 이벤트 발행 확인 (AbstractAggregateRoot 기능)
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(IdentityCreatedEvent.class);
    }

    @Test
    @DisplayName("소셜 회원가입 성공 시 비밀번호는 null이며, GUEST 권한을 갖는다.")
    void socialSignUp_success() {
        // given
        String email = "social@test.com";

        // when
        Identity identity = Identity.socialSignUp(email, TEST_PROFILE);

        // then
        assertThat(identity.getEmail()).isEqualTo(email);
        assertThat(identity.getPassword()).isNull(); // 소셜은 비밀번호 없음
        assertThat(identity.getRole()).isEqualTo(Roles.GUEST);

        // 이벤트 발행 확인
        List<Object> events = DomainEventsTestUtils.getEvents(identity);
        assertThat(events).isNotEmpty();
    }

    @Test
    @DisplayName("소셜 계정 연동 시 oauth 리스트에 추가되어야 한다.")
    void linkSocialAccount_success() {
        // given
        Identity identity = Identity.signUp("test@test.com", "pw", TEST_PROFILE);

        // when
        identity.linkSocialAccount(AuthProvider.GOOGLE, "google_12345");

        // then
        assertThat(identity.getOauths()).hasSize(1);
        assertThat(identity.getOauths().get(0).getOauthUserId()).isEqualTo("google_12345");
        assertThat(identity.getOauths().get(0).getOauthProvider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    @DisplayName("이미 연동된 소셜 제공자로 또 연동하려 하면 예외가 발생한다.")
    void linkSocialAccount_fail_duplicate() {
        // given
        Identity identity = Identity.signUp("test@test.com", "pw", TEST_PROFILE);
        identity.linkSocialAccount(AuthProvider.GOOGLE, "google_12345");

        // when & then
        // IdentityPolicy에서 중복 체크를 한다고 가정
        assertThatThrownBy(() -> identity.linkSocialAccount(AuthProvider.GOOGLE, "google_new_id"))
                .isInstanceOf(IdentityException.class); // 혹은 IdentityPolicy에서 던지는 구체적 예외
    }

    @Test
    @DisplayName("비밀번호 변경 시 새로운 비밀번호로 업데이트되어야 한다.")
    void changePassword_success() {
        // given
        Identity identity = Identity.signUp("test@test.com", "oldPw", TEST_PROFILE);
        String newPw = "NewPassword123!";

        // when
        identity.changePassword(newPw);

        // then
        assertThat(identity.getPassword()).isEqualTo(newPw);
    }

    @Test
    @DisplayName("회원 탈퇴 시 unsubscribe 상태가 true로 변경되어야 한다.")
    void withdraw_success() {
        // given
        Identity identity = Identity.signUp("test@test.com", "pw", TEST_PROFILE);

        // when
        identity.withdraw();

        // then
        assertThat(identity.isUnsubscribe()).isTrue();
    }

    @Test
    @DisplayName("탈퇴한 회원이 정보를 수정하려 하면 예외가 발생한다.")
    void fail_when_unsubscribed_user_action() {
        // given
        Identity identity = Identity.signUp("test@test.com", "pw", TEST_PROFILE);
        identity.withdraw(); // 탈퇴 처리

        // when & then
        assertThatThrownBy(() -> identity.changePassword("newPw"))
                .isInstanceOf(IdentityException.class)
                .hasMessage(IdentityErrorCode.UNSUBSCRIBED.getMessage()); // IdentityErrorCode 메시지 확인
    }

    @Test
    @DisplayName("멤버십 승급과 강등이 정상적으로 동작해야 한다.")
    void membership_promote_demote() {
        // given
        Identity identity = Identity.signUp("test@test.com", "pw", TEST_PROFILE);
        assertThat(identity.getRole()).isEqualTo(Roles.GUEST);

        // when (승급)
        identity.promoteToMember();
        // then
        assertThat(identity.getRole()).isEqualTo(Roles.MEMBER);

        // when (강등)
        identity.demoteToGuest();
        // then
        assertThat(identity.getRole()).isEqualTo(Roles.GUEST);
    }

    @Test
    @DisplayName("잘못된 이메일 형식이면 예외가 발생해야 한다.")
    void signUp_fail_invalid_email() {
        // given
        String invalidEmail = "invalid-email-format"; // @가 없음

        // when & then
        assertThatThrownBy(() -> Identity.signUp(invalidEmail, "Password123!", TEST_PROFILE))
                .isInstanceOf(IdentityException.class) // 혹은 InvalidInputException
                .hasMessageContaining("이메일"); // 에러 메시지에 '이메일' 관련 내용이 있는지
    }

}