package com.gymproject.common.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(){
        return JsonMapper.builder()
                .addModule(new JavaTimeModule()) // JavaTimeModule이 자동으로 Json(String)을 파싱해서 OffsetDateTime으로 변환해줌
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) //역직렬화시에 DTO 클래스에 매칭되는 필드없을때 무시
                .build();
    }
}
/*
	•	"2025-03-01T00:00:00+09:00" 문자열로 들어온걸 OffsetDateTime으로 파싱
 */
