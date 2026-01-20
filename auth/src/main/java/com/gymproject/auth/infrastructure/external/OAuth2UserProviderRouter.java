package com.gymproject.auth.infrastructure.external;

import com.gymproject.common.security.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

;

@RequiredArgsConstructor
@Component
public class OAuth2UserProviderRouter implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final GoogleOAuth2UserService googleOAuth2UserService;
//    private final AppleOAuth2UserService appleOAuth2UserService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String providerId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.fromRegistrationId(providerId);

        return switch (provider) {
            case GOOGLE -> googleOAuth2UserService.loadUser(userRequest);
//            case APPLE -> appleOAuth2UserService.loadUser(userRequest);
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
        };
    }
}

/*
    OAuth2UserProviderRouter는 OAuth2 로그인 과정에서 스프링 시큐리티가 구글에서 토큰을 받아온 직후 자동으로 호출되는 클래스.

    OAuth2UserService는 OAuth2 로그인 완료 후, 사용자 정보를 가져오는 역할을 하는 인터페이스임.
    사용자가 구글/애플 화면에서 로그인 허용을 누른 다음 Authorization code를 받아오면
    그 코드을 이용해서 Access Token을 구글에서 가져와야함.
    그리고 토큰을 이용해서 사용자 정보 가져옴.
    이때 OAuth2UserService가 담당함.

    userRequest안에는 다음과 같은 정보가 있음(이객에 하나에 OAuth 로그인에 필요한 모든 정보다 담김)
    1) clientRegistration : provider가 누구인지(google, apple..) + client_id, userInfoUri 등
    2) accessToken: 구글에서 발급한 access Token
    3) additionalParameters: 요청에 사용된 다른 파라미터들

    String providerId = userRequest.getClientRegistration() // 여기 까지는 application.yml에서 설정한 provider 정보 객체
                                    .getRegistrationId() // 여기는 "google", "apple"과 같은 문자열이 반환됨

e.g> providerId = "google"



    -- Big Flow --
    1) 사용자가 구글에 먼저 로그인 요청
    2) 구글이 로그인 성공과 함께 Authorization Code를 반환
e.g)  https://yourapp.com/login/oauth2/code/google?code=abc123
    3) code를 이용해 Access Token발급(스프링 시큐리티에서 자동으로 실행, DefaultAuthorizationCodeTokenResponseClient라는 클래스가 역할을 함)
    4) 토큰은 OAuth2UserRequest 객체에 저장함.
    5) 이 토큰으로 내 커스텀 OAuth2UserService로 전달.
 e.g> @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
    OAuth2AccessToken accessToken = userRequest.getAccessToken();
    }
    4) 토큰을 이용해 구글에 정보 요청
    4) 사용자에게 jwt 발급

    --Small Flow--
    1) 사용자가 구글 로그인 요청 - OAuth2LoginAuthenticationFilter 에서 처리
    2) 구글에서 Authorization Code 반환 - 내부 자동 처리
    3) 스프링 Access Token 교환요청 - 내부 자동 처리
    4) Access Token을 이용해 사용자 정보 요청 - OAuth2UserService.loadUser() 호출 // 현재 부분
    5) 받은 사용자 정보를 OAuth2User객체로 반환 - DefaultOAuth2UserService(기본 구현체임)

    만약 내가 Custom하지않으면 기본 구현체를 반환함. 보통은 db에 저장/생성/토큰 발급 등의 추가로직때문에
    커스텀 구현체를 만듦.



 */
