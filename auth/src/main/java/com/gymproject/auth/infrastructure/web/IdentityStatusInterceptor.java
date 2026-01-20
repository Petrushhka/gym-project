package com.gymproject.auth.infrastructure.web;

import com.gymproject.auth.application.service.IdentityService;
import com.gymproject.auth.domain.entity.Identity;
import com.gymproject.auth.exception.IdentityErrorCode;
import com.gymproject.auth.exception.IdentityException;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.security.MemberOnly;
import com.gymproject.common.security.Roles;
import com.gymproject.common.security.TrainerOnly;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class IdentityStatusInterceptor implements HandlerInterceptor {

    private final IdentityService identityService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. SecurityContext에서 현재 로그인 유저 ID를 가져옴
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 로그인 하지 않은 접근(익명 사용자) 통과
        if(authentication == null || !(authentication.getPrincipal() instanceof UserAuthInfo userAuthInfo)){
            return true;
        }

        // 2. 실시간 DB 조회 및 탈퇴 여부 확인
        Identity identity = identityService.findById(userAuthInfo.getUserId());
        identity.checkUnsubscribed();

        // 3. 역할 (ROLE) 검증
        if(handler instanceof HandlerMethod handlerMethod){
            if(handlerMethod.hasMethodAnnotation(TrainerOnly.class)){
                validateRole(identity, Roles.TRAINER);
            }
            if(handlerMethod.hasMethodAnnotation(MemberOnly.class)){
                validateRole(identity, Roles.MEMBER);
            }
        }

        return true;
    }


    private void validateRole(Identity identity, Roles role){
        if(identity.getRole() != role){
            throw new IdentityException(IdentityErrorCode.NOT_AUTHORITY);
        }
    }

}

/*
    [중요]
    Oauth2SuccessHandler 또는 IdentityStatusInterceptor는 HTTP 요청/응답을 다루는 웹 기술 의존적인 컴포넌트임
    외부 요청이 들어오는 Web계층의 진입점(Inbound Adapter)에 가까움

    이 인터셉터는 Spring Security의 필터가 아님, Spring MVC의 HandlerInterceptor 임
    따라서 WebMvcConfig라는 별도의 설정 파일에 등록해야함. 이게 없다면 스프링은 이 인터셉터의 존재를 모름(api 모듈쪽에 있음)

 */