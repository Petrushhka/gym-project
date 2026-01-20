package com.gymproject.auth.infrastructure.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymproject.common.dto.exception.CommonResDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // 1. Filter에서 달아놓은 꼬리표 확인
        String exceptionCode = (String) request.getAttribute("exception");
        String errorCode = "UNAUTHORIZED";
        String message = "Security 설정이 필요합니다.";

        if ("EXPIRED_TOKEN".equals(exceptionCode)) {
            errorCode = "TOKEN_EXPIRED";
            message = "토큰이 만료되었습니다.";
        } else if ("INVALID_TOKEN".equals(exceptionCode)) {
            errorCode = "TOKEN_INVALID";
            message = "유효하지 않은 토큰입니다.";
        }

        log.warn("인증 실패 - Code: {}, Message: {}", errorCode, message);

        CommonResDto<?> commonResDto
                = CommonResDto.error(
                401,
                errorCode,
                message);


        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        objectMapper.writeValue(response.getWriter(), commonResDto);
    }
}

/*
    유효하지 않은 토큰으로 접근하면 401 응답
    e.g) 토큰이 없음, 만료, 서명이 잘못된 경우
    JwtAuthFilter에서 예외를 잡지 않고, SecurityContext가 비어있으면,
    spring의 ExceptionTranslationFilter가 이 EntryPoint를 호출
    응답
 */
