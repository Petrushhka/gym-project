package domain;

import com.gymproject.classmanagement.template.domain.entity.Template;
import com.gymproject.classmanagement.template.domain.type.ClassKind;
import com.gymproject.classmanagement.template.domain.type.RecommendLevel;
import com.gymproject.classmanagement.template.exception.TemplateErrorCode;
import com.gymproject.classmanagement.template.exception.TemplateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateTest {

    @Test
    @DisplayName("성공: 템플릿 생성 시 데이터가 정상적으로 매핑된다")
    void create_success() {
        // Policy가 통과한다는 가정하에, 엔티티 필드가 잘 들어가는지만 확인
        Template template = Template.create(
                "필라테스", "설명", 10, 50, RecommendLevel.BEGINNER, ClassKind.GROUP
        );

        assertThat(template.getTitle()).isEqualTo("필라테스");
        assertThat(template.getCapacity()).isEqualTo(10);
    }

    @Test
    @DisplayName("성공: 수정(Update) 시 필드 값이 변경된다")
    void update_success() {
        // given
        Template template = Template.create("원래제목", "설명", 10, 50, RecommendLevel.BEGINNER, ClassKind.GROUP);

        // when
        template.update("바뀐제목", "새설명", 10, 50, RecommendLevel.INTERMEDIATE, ClassKind.GROUP);

        // then
        assertThat(template.getTitle()).isEqualTo("바뀐제목");
        assertThat(template.getRecommendLevel()).isEqualTo(RecommendLevel.INTERMEDIATE);
    }

    @Test
    @DisplayName("실패: 이미 삭제된 템플릿을 수정하면 예외 발생")
    void update_fail_deleted() {
        // given
        Template template = Template.create("제목", "설명", 10, 50, RecommendLevel.BEGINNER, ClassKind.GROUP);
        template.softDelete(OffsetDateTime.now()); // 삭제 처리

        // when & then
        assertThatThrownBy(() ->
                template.update("수정시도", "설명", 10, 50, RecommendLevel.BEGINNER, ClassKind.GROUP))
                .isInstanceOf(TemplateException.class)
                .hasFieldOrPropertyWithValue("errorCode", TemplateErrorCode.ALREADY_DELETED.getErrorCode());
    }

}