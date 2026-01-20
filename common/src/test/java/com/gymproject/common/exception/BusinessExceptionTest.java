package com.gymproject.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    // 테스트용 더미 예외 (BusinessException은 추상클래스라 직접 만들지 못함)
    static class SampleBusinessException extends BusinessException {
        SampleBusinessException(String message, int statusCode, String errorCode) {
            super(message, statusCode, errorCode);
        }

        SampleBusinessException(String message, int statusCode, String errorCode, Throwable cause) {
            super(message, statusCode, errorCode, cause);
        }
    }

    @Nested
    @DisplayName("BusinessException - 성공 케이스")
    class ExceptionSuccess {

        @Test
        @DisplayName("message, statusCode, errorCode를 저장한다.")
        void store_fields() {
            // given
            String message = "에러 메시지";
            int statusCode = 400;
            String errorCode = "COMMON_001";

            // when
            BusinessException ex = new SampleBusinessException(message, statusCode, errorCode);

            // then
            assertThat(ex.getMessage()).isEqualTo(message);
            assertThat(ex.getStatusCode()).isEqualTo(statusCode);
            assertThat(ex.getErrorCode()).isEqualTo(errorCode);
        }

        @Test
        @DisplayName("cause를 RuntimeException까지 전달한다.")
        void store_cause(){
            //given
            Throwable cause = new RuntimeException("root cause");

            // when
            BusinessException ex = new SampleBusinessException("message", 500, "COMMON_123", cause);

            // then
            assertThat(ex.getCause()).isSameAs(cause); // isSameAs: 객체
        }
    }


}