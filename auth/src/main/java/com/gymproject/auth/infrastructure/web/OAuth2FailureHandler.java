package com.gymproject.auth.infrastructure.web;

import com.gymproject.auth.application.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AuthService authService;

    private static final Logger log = LoggerFactory.getLogger(OAuth2FailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        log.warn("OAuth2 인증 실패: {}", exception.getMessage(), exception);
        // 서비스로부터 리다이렉트 URI 획득
        String redirectUrl = authService.processOAuthFailure(exception);

        // 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
