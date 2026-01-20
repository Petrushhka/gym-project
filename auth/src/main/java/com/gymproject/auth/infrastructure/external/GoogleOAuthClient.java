package com.gymproject.auth.infrastructure.external;

import com.gymproject.auth.infrastructure.adapter.dto.GoogleTokenResponse;
import com.gymproject.auth.infrastructure.adapter.dto.GoogleUserInfo;
import com.gymproject.common.exception.auth.OAuthCommunicationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


@Component
@RequiredArgsConstructor
public class GoogleOAuthClient {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RestTemplate restTemplate;

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);

    // https://developers.google.com/identity/protocols/oauth2/web-server?hl=ko#httprest_1 << 토큰 요청하는 부분 참조
    public GoogleTokenResponse getAccessToken(String registrationId, String authCode) {
        // yml에서 설정된 google Registration정보를 읽어옴
        ClientRegistration registration =
                clientRegistrationRepository.findByRegistrationId(registrationId);

        if (registration == null) {
            throw new OAuth2AuthenticationException("google yaml 설정에 없음" + registration);
        }

        String tokenUrl =
                registration.getProviderDetails().getTokenUri();

        //google에서 json형식이 아닌 x-www-from-urlencoded 방식으로만 요청을 받음
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", authCode);
        params.add("redirect_uri", registration.getRedirectUri()); //[중요] 구글 개발자 콘솔에 리다이렉트 URI 등록된거랑 확인해보기
        params.add("client_id", registration.getClientId());
        params.add("client_secret", registration.getClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        try{
        ResponseEntity<GoogleTokenResponse> response =
                restTemplate.postForEntity(tokenUrl, request, GoogleTokenResponse.class);

        return response.getBody();}catch(RestClientException e){
            log.error("Google Access Token 요청 실패. authCode: {}, Error: {}", authCode, e.getMessage());
            throw new OAuthCommunicationException("Google 서버에서 Access Token을 가져오는 데 실패했습니다.", e);
        }
    }

    public GoogleUserInfo getUserInfo(String registrationId, String token) {
        ClientRegistration registration =
                clientRegistrationRepository.findByRegistrationId(registrationId);

        String userInfoUri = registration.getProviderDetails()
                .getUserInfoEndpoint()
                .getUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<String> request = new HttpEntity<>(headers);
        try {
        ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(
                userInfoUri,
                HttpMethod.GET,
                request,
                GoogleUserInfo.class
        );

        return response.getBody();
        } catch (RestClientException e) {
            //  Google 통신 실패 시 (ex: 401 - 토큰 만료 등)
            log.error("Google User Info 요청 실패. AccessToken: {}, Error: {}", token, e.getMessage());
            // 기술적인 예외를 우리가 만든 비즈니스 예외로 "번역"해서 던집니다.
            throw new OAuthCommunicationException("Google 서버에서 사용자 정보를 가져오는 데 실패했습니다.", e);
        }

    }

    /*
    google에서 우리에게 주는 유저정보 객체 형태
    {
  "id": "10923812093812093", << 이게 필요한거임
  "email": "example@gmail.com",
  "verified_email": true,
  "name": "John Doe",
  "given_name": "John",
  "family_name": "Doe",
  "picture": "https://lh3.googleusercontent.com/a/AAcHTtdxxxxxxxx",
  "locale": "en"
    }
     */

}

