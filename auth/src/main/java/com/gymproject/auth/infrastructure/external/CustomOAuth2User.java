package com.gymproject.auth.infrastructure.external;

import com.gymproject.common.security.AuthProvider;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {

    //    private final User user;
    private final Map<String, Object> attributes;
    private final AuthProvider provider;
    private final String providerId;


    public CustomOAuth2User(Map<String, Object> attributes,
                            AuthProvider provider,
                            String providerId) {
        this.attributes = attributes;
        this.provider = provider;
        this.providerId = providerId;
    }

    @Override
    public String getName() {
        return providerId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Oauth인증 성공은 로그인 흐름의 시작임, role을 알수 없으므로 start라고 표현
        // role 판단은 successHandler에서 identity모듈 호출후 결정
        return List.of(new SimpleGrantedAuthority("START"));
    }
}

/*
    OAuth2 로그인 -> OAuth2User 생성 -> Authentication 객체 생성 -> SecurityContextHolder에 저장



    해당 클래스는 spring security 가 oauth 로그인 성공 후 관리한 authentication principal(=로그인된 사용자 정보)를 담는 껍데기 객체를 만드는 클래스

    로그인한 사람 정보
    어떤 provider로 로그인했는지
    provider에서 받은 이메일, 닉네임 등 정보

    를 종합하여 SecurityContextHolder에 저장해서 전체 시스템에서 사용가능하게 만드는 역할을함


    사용하는 이유는

    기본 OAuth2User는 한 개의 Provider 기준으로만 동작함
    구글은 email 정보를 주지만, 애플은 주지 않음
    카카오는 구조자체가 아예 다름

    provider마다 사용자 정보 구조가 다름

    그래서 표준화된 사용자 정보로 만들어주기위한 클래스임

    @Override
    public String getName() {
        return String.valueOf(user.getUserId());
    }
    -> Spring Security 내부에는 인증된 사용자를 구분하는 기주값이 필요함.
    기본 Oauth2User는 "sub"같은 provider_user_id를 name으로 반환함.
    하지만 오버라이딩해서 그냥 User객체의 userId가 나오게하는것임.
    SprintConetxt.principal.getName()하면 userId 값이 나오게 됨

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole().name()));
    }
    -> 핵심임.
    Spring security는 인증과 인가를 구분함
    인증 = 누구?
    인가 = 권한

    인가를 하려면 결국 Role형태의 권한 정보가 필요함.
    User 엔터티에 Role이 존재함.
    따라서 "Role_" + user.getRole().name()으로해서 spring Security에서 읽을 수 있도록 바꿔준것





 */