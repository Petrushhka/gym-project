package com.gymproject.auth.infrastructure.external;

import com.gymproject.common.security.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuth2UserService.class);

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        AuthProvider provider = AuthProvider.fromRegistrationId(userRequest.getClientRegistration().getRegistrationId());
        String providerId = oAuth2User.getAttribute(provider.getProviderCode());

        log.debug("OAuth2UserService Loaded User. Provider: {}, ProviderId: {}", provider, providerId);

        return new CustomOAuth2User(
                oAuth2User.getAttributes(),
                provider,
                providerId
        );
    }
}

/*
    DefualtOAuth2UserService는 spring이 기본 제공하는 Access Token으로 사용자 정볼르 요청하는 기본 구현체.
    super.loadUser()를 호출하면,
    Spring이 알아서 구글에 https//www.~~~/oauth2/v3/userinfo 엔드포인트로 요청을 보내고
    사용자 정보(json)을 OAuth2User로 반환해줌.

    1) AuthProvider provider = AuthProvider.valueOf(userRequest.getClientRegistration().getRegistrationId());
    -> 현재 로그인 요청이 어떤 provider로 부터 왔는지 구분.

     2) String providerId = oAuth2User.getAttribute(provider.getProviderCode());
    -> 실상은 다음과 같이 써도 무방함.
 e.g> String providerId = oAuth2User.getAttribute("email")
    -> 하지만 위와 같이 사용하면, 응답형태에 맞춰서 메서드를 맞춰줘야함. 따라서 Enum에서 자동으로 키값이 맵핑되어있으니 그걸 이용한것.





 */
