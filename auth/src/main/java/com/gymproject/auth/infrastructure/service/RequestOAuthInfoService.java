package com.gymproject.auth.infrastructure.service;

import com.gymproject.auth.infrastructure.adapter.dto.GoogleTokenResponse;
import com.gymproject.auth.infrastructure.adapter.dto.GoogleUserInfo;
import com.gymproject.auth.infrastructure.external.GoogleOAuthClient;
import com.gymproject.common.security.AuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestOAuthInfoService {

    private final GoogleOAuthClient googleOAuthClient;

    // 외부에서 호출하는 메서드 (Code -> ProviderId 반환)
    public String getProviderId(AuthProvider provider, String authCode) {
        GoogleUserInfo userInfo = getGoogleUserInfo(provider, authCode);
        userInfo.checkNullProviderId();
        return userInfo.getProviderId();
    }


    private GoogleUserInfo getGoogleUserInfo(AuthProvider provider, String authCode) {
        String registrationId = getRegistrationId(provider);

        // 구글 서버와 통신 (Access Token 발급)
        GoogleTokenResponse tokenResponse =
                googleOAuthClient.getAccessToken(registrationId, authCode);

        // 구글 서버와 통신(유저 정보 획득)
        return googleOAuthClient.getUserInfo(registrationId, tokenResponse.getAccessToken());
    }

    private String getRegistrationId(AuthProvider provider) {
        return switch(provider){
            case GOOGLE -> "google-link";
            case APPLE ->  "apple-link";
            default -> throw new IllegalArgumentException("지원하지 않는 Provider " + provider);
        };
    }
}
