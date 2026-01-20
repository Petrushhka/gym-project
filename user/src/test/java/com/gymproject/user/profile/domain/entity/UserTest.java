package com.gymproject.user.profile.domain.entity;

import com.gymproject.common.security.SexType;
import com.gymproject.user.domain.entity.util.DomainEventsTestUtils;
import com.gymproject.user.profile.domain.event.UserJoinedEvent;
import com.gymproject.user.profile.domain.vo.PhoneNumber;
import com.gymproject.user.profile.exception.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class UserTest {

    // 테스트용 상수
    private static final Long ID = 1L;
    private static final String FIRST = "GILDONG";
    private static final String LAST = "HONG";

    // 호주 형식: 04로 시작하는 모바일 번호
    private static final String VALID_PHONE_RAW = "0412 345 678";
    private static final String VALID_PHONE_CLEAN = "0412345678";

    @Nested
    @DisplayName("1. PhoneNumber VO(값 객체) 테스트")
    class PhoneNumberTest {

        @Test
        @DisplayName("공백이나 하이픈이 섞인 호주 번호를 넣으면 숫자만 남겨서 생성된다.")
        void create_success_normalization() {
            // given
            String raw = "0412-345-678";

            // when
            PhoneNumber phone = new PhoneNumber(raw);

            // then
            assertThat(phone.value()).isEqualTo("0412345678");
        }

        @Test
        @DisplayName("+61(국가코드)가 포함된 번호도 정상적으로 생성된다.")
        void create_success_internaltional() {
            // given
            String raw = "+61 412 345 678";

            // when
            PhoneNumber phone = new PhoneNumber(raw);

            // then
            assertThat(phone.value()).isEqualTo("+61412345678");
        }

        @Test
        @DisplayName("형식에 맞지 않는 번호(짧음/패턴불일치)는 예외가 발생한다.")
        void create_fail_invalid_pattern() {
            // given
            String invalidPhone = "010-1234-5678"; // 한국 번호 (호주 패턴 아님)

            // when & then
            assertThatThrownBy(() -> new PhoneNumber(invalidPhone))
                    .isInstanceOf(UserException.class)
                    .hasFieldOrPropertyWithValue("errorCode", "INVALID_PHONE_FORMAT");
        }

        @Test
        @DisplayName("빈 문자열이나 null이 들어오면 예외가 발생한다.")
        void create_fail_empty() {
            assertThatThrownBy(() -> new PhoneNumber(""))
                    .isInstanceOf(UserException.class);

            assertThatThrownBy(() -> new PhoneNumber(null))
                    .isInstanceOf(UserException.class);
        }

        @Nested
        @DisplayName("2. User Entity 및 Policy 테스트")
        class UserEntityTest {

            @Test
            @DisplayName("유저 등록 성공: VO를 받아 생성되고 이벤트가 발행된다.")
            void register_success() {
                // given
                PhoneNumber phone = new PhoneNumber(VALID_PHONE_RAW);

                // when
                User user = User.registUser(ID, FIRST, LAST, phone, SexType.MALE);

                // then
                assertThat(user.getUserId()).isEqualTo(ID);
                assertThat(user.getFirstName()).isEqualTo(FIRST);
                assertThat(user.getPhoneNumber()).isEqualTo(phone); // 객체 비교
                assertThat(user.getPhoneNumber().value()).isEqualTo(VALID_PHONE_CLEAN); // 값 비교

                // 이벤트 확인
                List<Object> events = DomainEventsTestUtils.getEvents(user);
                assertThat(events).hasSize(1);
                assertThat(events.get(0)).isInstanceOf(UserJoinedEvent.class);
            }

            @Test
            @DisplayName("프로필 수정 성공: 이름과 전화번호(VO)가 변경된다.")
            void update_profile_success() {
                // given
                User user = User.registUser(ID, FIRST, LAST, new PhoneNumber(VALID_PHONE_RAW), SexType.MALE);
                PhoneNumber newPhone = new PhoneNumber("0499 111 222");

                // when
                user.updateProfile("UpdatedFirst", "UpdatedLast", newPhone);

                // then
                assertThat(user.getFirstName()).isEqualTo("UpdatedFirst");
                assertThat(user.getLastName()).isEqualTo("UpdatedLast");
                assertThat(user.getPhoneNumber()).isEqualTo(newPhone);
            }

            @Test
            @DisplayName("프로필 수정 시에도 이름 정책을 위반하면 예외가 발생한다.")
            void update_fail_invalid_name() {
                // given
                User user = User.registUser(ID, FIRST, LAST, new PhoneNumber(VALID_PHONE_RAW), SexType.MALE);
                String invalidName = "Hajun@";

                // when & then
                assertThatThrownBy(() -> user.updateProfile(invalidName, "ValidLast", null))
                        .isInstanceOf(UserException.class)
                        .hasFieldOrPropertyWithValue("errorCode", "INVALID_NAME_FORMAT");
            }
            @Test
            @DisplayName("프로필 수정 시 null을 넣으면 해당 필드는 변경되지 않는다.")
            void update_profile_partial_update() {
                // given
                PhoneNumber originalPhone = new PhoneNumber(VALID_PHONE_RAW);
                User user = User.registUser(ID, FIRST, LAST, originalPhone, SexType.MALE);

                // when (전화번호에 null 전달)
                user.updateProfile("NewName", null, null);

                // then
                assertThat(user.getFirstName()).isEqualTo("NewName"); // 변경됨
                assertThat(user.getLastName()).isEqualTo(LAST);       // 유지됨
                assertThat(user.getPhoneNumber()).isEqualTo(originalPhone); // 유지됨
            }

            @Test
            @DisplayName("이름 정책 위반: 특수문자가 들어가면 예외가 발생한다.")
            void policy_fail_invalid_name() {
                // given
                PhoneNumber phone = new PhoneNumber(VALID_PHONE_RAW);
                String invalidName = "Hajun@";

                // when & then
                assertThatThrownBy(() -> User.registUser(ID, invalidName, LAST, phone, SexType.MALE))
                        .isInstanceOf(UserException.class)
                        .hasFieldOrPropertyWithValue("errorCode", "INVALID_NAME_FORMAT");
            }

            @Test
            @DisplayName("이름 정책 위반: 이름이 너무 길면(50자 초과) 예외가 발생한다.")
            void policy_fail_long_name() {
                // given
                PhoneNumber phone = new PhoneNumber(VALID_PHONE_RAW);
                String longName = "A".repeat(51);

                // when & then
                assertThatThrownBy(() -> User.registUser(ID, longName, LAST, phone, SexType.MALE))
                        .isInstanceOf(UserException.class)
                        .hasFieldOrPropertyWithValue("errorCode", "INVALID_NAME_FORMAT");

            }
        }
    }



}