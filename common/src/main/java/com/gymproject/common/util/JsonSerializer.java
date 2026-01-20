package com.gymproject.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymproject.common.exception.InvalidJsonInputException;
import com.gymproject.common.exception.JsonConvertFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/*
    DTO -> JSON 직렬화(Serialization)
    JSON -> DTO 역직렬화Deserialization)
 */
@Component
@RequiredArgsConstructor
public class JsonSerializer {

    private final ObjectMapper objectMapper;

    // 직렬화
    public String serialize(Object object) {
        if (object == null) {
            throw new InvalidJsonInputException("직렬화 대상이 null 입니다.");
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonConvertFailedException("직렬화 실패", e);
        }
    }

    // 역직렬화
    public <T> T deserialize(String json, Class<T> clazz) {
        if (clazz == null) {
            throw new InvalidJsonInputException("역직렬화 대상 타입(clazz)이 null 입니다.");
        }
        if (json == null || json.isBlank()) {
            throw new InvalidJsonInputException("역질렬화 JSON이 비어있습니다.");
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new JsonConvertFailedException("역질렬화 실패", e);
        }
    }




    /*
        역직렬화시에 DTO 클래스에 매칭되는 필드가 없으면? 에러가 나는지? [중요]
        에러 안나고, 자동으로 무시됨
        Boot에서 FAIL_ON_UNKOWN_PROPERTIES = false 로 설정됨

        오히려 DTO 하나로 결제 세부 내역을 관리하는게 더 편해짐
     */

    /*
        null/blank가 와도 BusinessException을 항상 보장할 수 있게 수정
     */

}
