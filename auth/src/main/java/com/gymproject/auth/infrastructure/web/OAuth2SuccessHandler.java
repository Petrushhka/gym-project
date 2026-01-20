package com.gymproject.auth.infrastructure.web;

import com.gymproject.auth.application.service.AuthService;
import com.gymproject.auth.application.dto.response.OAuthLoginResponse;
import com.gymproject.auth.infrastructure.external.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // 신규 유저인지, 리다이렉트주소, 토큰
        OAuthLoginResponse loginResponse = authService.processOAuthLogin(oAuth2User);

        log.debug("OAuth2 SuccessHandler 실행. UserAuthInfo: {}", loginResponse.userAuthInfo());

        // 결정된 URI로 보냄
        getRedirectStrategy().sendRedirect(request, response, loginResponse.redirectUrl());
    }
}
