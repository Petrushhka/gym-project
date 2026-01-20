package com.gymproject.api.config;

import com.gymproject.auth.infrastructure.web.IdentityStatusInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final IdentityStatusInterceptor identityStatusInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(identityStatusInterceptor)
                .addPathPatterns("/**") // 모든 API에 적용하되
                .excludePathPatterns("/login"); // 로그인 검사는 제외
    }
}
