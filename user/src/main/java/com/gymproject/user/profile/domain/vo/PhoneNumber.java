package com.gymproject.user.profile.domain.vo;

import com.gymproject.user.profile.exception.UserErrorCode;
import com.gymproject.user.profile.exception.UserException;
import jakarta.persistence.Column;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public record PhoneNumber (
        @Column(name = "phone_number", nullable= false)
        String value
){
    private static final Pattern AUS_PHONE_PATTERN = Pattern.compile("^(?:\\+61|0)[2-478]\\d{8}$");

public PhoneNumber{
    // 1. 필수값 체크
    if(!StringUtils.hasText(value)){
        throw new UserException(UserErrorCode.INVALID_PHONE_FORMAT);
    }

    // 2. 정제(Normalizaion): 숫자와 '+'만 남김
    value = value.replaceAll("[^0-9+]", "");

    // 3. 검증
    if(!AUS_PHONE_PATTERN.matcher(value).matches()){
    throw new UserException(UserErrorCode.INVALID_PHONE_FORMAT);
    }
}
    // toString을 오버라이딩해서 로그 찍을때 편하게
    @Override
    public String toString() {
        return value;
    }
}
