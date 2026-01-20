package com.gymproject.common.exception;

import com.gymproject.common.exception.auth.OAuthCommunicationException;
import com.gymproject.common.exception.user.InvalidCredentialsException;
import com.gymproject.common.exception.user.NotMemberException;
import com.gymproject.common.exception.user.NotTrainerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonExceptionContractTest {

    @Nested
    @DisplayName("BusinessException 자식들 - 규약 테스트")
    class Contract{

        @Test
        @DisplayName("statusCode는 400~599 범위여아 한다.")
        void status_code_range() {
            // given
            BusinessException[] exceptions = new BusinessException[]{
                    new DuplicateUserException("dup"),
                    new InvalidCredentialsException("bad"),
                    new NotMemberException("not_member"),
                    new NotTrainerException("not_trainer"),
                    new InvalidJsonInputException("invalid_json_input"),
                    new JsonConvertFailedException("conver fail", new RuntimeException("cause")),
                    new OAuthCommunicationException("oauth fail", new RuntimeException("casue"))
            };
            // when & then
            for(BusinessException exception : exceptions){
                assertThat(exception.getStatusCode())
                        .as(exception.getClass().getName() + " statusCode")
                        .isBetween(400, 599);
            }
            /*
                as() : 테스트 실패시 어떤 케이스가 실패했는지 메세지가 나옴 -> as("DuplicateUserException statusCode")
                getClass() : 클래스 타입을 반환
                getSimpleName() : 클래스이름만 가져옴 (eg. DuplicateUserException)
                getName(): 패키지 포함한 전체이름 ("com.gymproject.common.exception.user.DuplicatedUserException")
             */
        }

        @Test
        @DisplayName("errorCode는 null/blank이면 안된다.")
        void error_code_not_blank() {
            // given
            BusinessException[] exceptions = new BusinessException[]{
                    new DuplicateUserException("dup"),
                    new InvalidCredentialsException("bad"),
                    new NotMemberException("not_member"),
                    new NotTrainerException("not_trainer"),
                    new InvalidJsonInputException("invalid_json_input"),
                    new JsonConvertFailedException("conver fail", new RuntimeException("cause")),
                    new OAuthCommunicationException("oauth fail", new RuntimeException("casue"))
            };
            // when & then
            for(BusinessException exception : exceptions){
                assertThat(exception.getErrorCode())
                .as(exception.getClass().getName() + " errorCode")
                        .isNotNull()
                        .isNotBlank();

            }
        }
    }
}
