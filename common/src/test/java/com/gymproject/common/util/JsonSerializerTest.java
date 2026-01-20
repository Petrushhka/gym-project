package com.gymproject.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymproject.common.exception.InvalidJsonInputException;
import com.gymproject.common.exception.JsonConvertFailedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class JsonSerializerTest {

    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();
    private final JsonSerializer jsonSerializer = new JsonSerializer(objectMapper);

    record SampleDto(String type, OffsetDateTime startAt) {
    }

    @Nested
    @DisplayName("serialize - 성공케이스")
    class SerializeSuccess {
        @Test
        @DisplayName("dto를 직렬화하면 json 문자열이 생성된다.")
        void serialize_success() throws JsonProcessingException {
            // given
            OffsetDateTime startAt = OffsetDateTime.parse("2018-04-20T00:00:00+00:00");
            SampleDto dto = new SampleDto("NEW", startAt);

            // when
            String json = jsonSerializer.serialize(dto);

            // then
            var node = objectMapper.readTree(json); // 컴파일러가 알아서 데이터 타입을 지정해줌.
            assertThat(node.get("type").asText()).isEqualTo("NEW");

            OffsetDateTime parsed = OffsetDateTime.parse(node.get("startAt").asText());
            assertThat(parsed).isEqualTo(startAt);
        }
    }

    @Nested
    @DisplayName("serialize - 실패 케이스")
    class SerializeFailure {

        @Test
        @DisplayName("직렬화 대상이 null 이면 InvalidJsonInputException이 발생한다.")
        void serialize_null_throws() {
            assertThatThrownBy(() -> jsonSerializer.serialize(null))
                    .isInstanceOf(InvalidJsonInputException.class);
        }
    }

    @Nested
    @DisplayName("deserialize - 성공 케이스")
    class DeserializeSuccess {
        @Test
        @DisplayName("정상 json을 역직렬화하면 dto로 변환된다.")
        void deserialize_success() {
            // given
            String json = """
                    { "type": "NEW", "startAt": "2018-04-20T00:00:00+00:00"}
                    """;
            // when
            SampleDto dto = jsonSerializer.deserialize(json, SampleDto.class);

            // then
            assertThat(dto.type).isEqualTo("NEW");
            assertThat(dto.startAt).isEqualTo(OffsetDateTime.parse("2018-04-20T00:00:00+00:00"));
        }

        @Test
        @DisplayName("dto에 없는 필드(unknow)가 있어도 역직렬화에 성공한다.")
        void deserialize_ignoreUnknownField() {
            // given
            String json = """
                    {"type":"NEW","startAt":"2018-04-20T00:00:00+00:00", "unknown":"x"}
                    """;
            // when
            SampleDto dto = jsonSerializer.deserialize(json, SampleDto.class);

            // then
            assertThat(dto.type).isEqualTo("NEW");
            assertThat(dto.startAt).isEqualTo(OffsetDateTime.parse("2018-04-20T00:00:00+00:00"));
        }
    }

    @Nested
    @DisplayName("deserialize - 실패케이스")
    class DeserializeFailure {
        @Test
        @DisplayName("json이 null이면 InvalidJsonInputException이 발생한다.")
        void deserialize_nullJson_throws(){
             assertThatThrownBy(()->jsonSerializer.deserialize(null, SampleDto.class))
                     .isInstanceOf(InvalidJsonInputException.class);
        }

        @Test
        @DisplayName("json이 빈 문자열이면 InvalidJsonInputException이 발생한다.")
        void deserialize_emptyJson_throws() {
            assertThatThrownBy(()->jsonSerializer.deserialize("", SampleDto.class))
                    .isInstanceOf(InvalidJsonInputException.class);
        }

        @Test
        @DisplayName("json이 공백 문자열이면 InvalidJsonInputException이 발생한다.")
        void deserialize_blankJson_throws() {
            assertThatThrownBy(()->jsonSerializer.deserialize(" ", SampleDto.class))
                    .isInstanceOf(InvalidJsonInputException.class);
        }

        @Test
        @DisplayName("clazz가 null이면 InvalidJsonInpuException이 발생한다.")
        void deserialize_nullClazz_throws() {
            assertThatThrownBy(()->jsonSerializer.deserialize("{}", null))
                    .isInstanceOf(InvalidJsonInputException.class);
        }

        @Test
        @DisplayName("깨진 json이면 JsonConvertFailedException이 발생한다.")
        void deserialize_invalidJson_throws() {
            // given
            String broken = "{";

            //when & then
            assertThatThrownBy(()->jsonSerializer.deserialize(broken, SampleDto.class))
                    .isInstanceOf(JsonConvertFailedException.class);
        }
    }
}