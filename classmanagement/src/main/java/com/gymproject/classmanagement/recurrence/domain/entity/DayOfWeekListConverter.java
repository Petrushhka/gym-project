package com.gymproject.classmanagement.recurrence.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.time.DayOfWeek;
import java.util.List;

/*
    JPA는 Text[]를 처리할 수 없어서 Converter가 필요
    DB -> Entity: TEXT[] -> List
    Entity -> DB: List -> TEXT[]
 */
@Converter
public class DayOfWeekListConverter implements AttributeConverter<List<DayOfWeek>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<DayOfWeek> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            // List<DayOfWeek>를 JSON 문자열로 직렬화
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            // 직렬화 실패 시 런타임 예외 발생
            throw new RuntimeException("요일 정보를 DB 저장을 위해 변환할 수 없습니다.", e);
        }
    }

    @Override
    public List<DayOfWeek> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            // JSON 문자열을 List<DayOfWeek> 객체로 역직렬화
            return objectMapper.readValue(dbData, new TypeReference<List<DayOfWeek>>() {});
        } catch (IOException e) {
            // 역직렬화 실패 시 런타임 예외 발생
            throw new RuntimeException("DB 정보를 Java 객체로 변환할 수 없습니다.", e);
        }
    }

}
