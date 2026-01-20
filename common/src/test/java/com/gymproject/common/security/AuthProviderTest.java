package com.gymproject.common.security;

import com.gymproject.common.exception.auth.UnsupportedRegistrationIdException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class AuthProviderTest {

    @Nested
    @DisplayName("fromRegistrationId - 성공 케이스")
    class Success{

        @Test
        @DisplayName("google 관련 registrationId는 GOOGLE을 반환한다.")
        void googleProvider() {
            // given
            String regId = "google-oauth2";

            // when
            AuthProvider result = AuthProvider.fromRegistrationId(regId);

            // then
            assertThat(result).isEqualTo(AuthProvider.GOOGLE);
        }

        @Test
        @DisplayName("apple 관련 registartionId는 APPLE을 반환한다.")
        void appleProvider() {
            // given
            String regId = "apple-login";

            // when
            AuthProvider result = AuthProvider.fromRegistrationId(regId);

            // then
            assertThat(result).isEqualTo(AuthProvider.APPLE);
        }
    }


    @Nested
    @DisplayName("fromRegistrationId - 실패 케이스")
    class Failure{
        @Test
        @DisplayName("지원하지 않는 registrationI는 예외를 던진다.")
        void unsupportedProvider() {
            // given
            String regId = "naver";

            // when
            assertThatThrownBy(()->AuthProvider.fromRegistrationId(regId))
                    .isInstanceOf(UnsupportedRegistrationIdException.class);
        }

        @Test
        @DisplayName("registrationId가 null이면 예외가 발생한다.")
        void nulRegistrationId() {
            assertThatThrownBy(()->AuthProvider.fromRegistrationId(null))
                    .isInstanceOf(UnsupportedRegistrationIdException.class);
        }

        @Test
        @DisplayName("registrationId가 빈 문자열이면 예외 발생")
        void emptyRegistrationId() {
            assertThatThrownBy(()->AuthProvider.fromRegistrationId(""))
                    .isInstanceOf(UnsupportedRegistrationIdException.class);
        }

        @Test
        @DisplayName("registrationId가 공백열이면 예외 발생")
        void blankRegistrationId() {
            assertThatThrownBy(()->AuthProvider.fromRegistrationId(" "))
                    .isInstanceOf(UnsupportedRegistrationIdException.class);
        }
    }
}