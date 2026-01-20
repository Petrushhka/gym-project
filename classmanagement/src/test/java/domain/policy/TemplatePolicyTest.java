package domain.policy;

import com.gymproject.classmanagement.template.domain.policy.TemplatePolicy;
import com.gymproject.classmanagement.template.domain.type.ClassKind;
import com.gymproject.classmanagement.template.exception.TemplateErrorCode;
import com.gymproject.classmanagement.template.exception.TemplateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplatePolicyTest {
    @Test
    @DisplayName("규칙: 개인 수업은 정원이 1명이어야 한다")
    void validate_personal_capacity() {
        // given
        int invalidCapacity = 2;

        // when & then
        assertThatThrownBy(() ->
                TemplatePolicy.validate(invalidCapacity, 60, ClassKind.PERSONAL))
                .isInstanceOf(TemplateException.class)
                .hasFieldOrPropertyWithValue("errorCode", TemplateErrorCode.INVALID_PERSONAL_CAPACITY.getErrorCode());
    }

    @Test
    @DisplayName("규칙: 수업 시간은 10분 단위여야 한다")
    void validate_duration_unit() {
        // when & then
        assertThatThrownBy(() ->
                TemplatePolicy.validate(10, 45, ClassKind.GROUP)) // 45분은 에러
                .isInstanceOf(TemplateException.class)
                .hasFieldOrPropertyWithValue("errorCode", TemplateErrorCode.INVALID_DURATION_UNIT.getErrorCode());
    }
}