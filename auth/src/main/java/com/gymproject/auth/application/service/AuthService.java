package com.gymproject.auth.application.service;

import com.gymproject.auth.application.dto.request.LoginRequest;
import com.gymproject.auth.application.dto.response.LoginResponse;
import com.gymproject.auth.application.dto.response.OAuthLoginResponse;
import com.gymproject.auth.application.port.TokenStoragePort;
import com.gymproject.auth.domain.entity.Identity;
import com.gymproject.auth.exception.IdentityErrorCode;
import com.gymproject.auth.exception.IdentityException;
import com.gymproject.auth.infrastructure.external.CustomOAuth2User;
import com.gymproject.auth.infrastructure.jwt.TokenProvider;
import com.gymproject.common.dto.auth.TokenResponse;
import com.gymproject.common.dto.auth.UserAuthInfo;
import com.gymproject.common.port.user.UserProfilePort;
import com.gymproject.common.security.AuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;


@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 인증과 관련된 로직(JWT,REDIS, Google)
 */
@Transactional
public class AuthService {

//    private final GoogleOAuthClient googleOAuthClient;
    private final IdentityService identityService; // 신원 확인 담당
    private final TokenStoragePort tokenStoragePort; // 저장소 담당
    private final TokenProvider tokenProvider; // 토큰 발행 담당
    private final UserProfilePort userProfilePort;

    @Value("${app.oauth.success-redirect-uri}")
    private String successRedirectUri;

    @Value("${app.oauth.signup-redirect-uri}")
    private String signupRedirectUri;

    @Value("${app.oauth.failure-redirect-uri}")
    private String failureRedirectUrl;

    // 1. 일반 로그인
    public LoginResponse login(LoginRequest dto) {
        // 1. 유저 모듈에 해당 유저의 정보가 맞는지 검증(IdentityService 위임)
        UserAuthInfo authInfo = identityService.verifyIdentity(
                dto.getEmail(),
                dto.getPassword());

        // 2. 검증된 정보를 바탕으로 토큰 발행(TokenProvider 위임)
        TokenResponse tokenResponse = tokenProvider.issueToken(authInfo);

        // 3. refresh를 위해 리프레시 토큰을 Redis에 저장(Port 위임)
        tokenStoragePort.updateRefreshToken(
                authInfo.getUserId(),
                tokenResponse.getRefreshToken(),
                tokenResponse.getRefreshTokenDuration()
        );

        // LoginResponse 생성
        LoginResponse loginResponse = LoginResponse.builder()
                .role(authInfo.getRole())
                .email(authInfo.getEmail())
                .userName(userProfilePort.getUserFullName(authInfo.getUserId()))
                .userId(authInfo.getUserId())
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .refreshTokenDuration(tokenResponse.getRefreshTokenDuration())
                .build();

        return loginResponse;
    }

    // 3. 소셜 연결(현재 구글만)
//    public OAuthLinkResponse linkSocialAccount(AuthProvider provider,
//                                               String authCode,
//                                               UserAuthInfo userAuthInfo) {
//        // 1. Google 서버에서 사용자 정보 가져오기
//        GoogleUserInfo googleUserInfo = getGoogleUserInfo(provider, authCode);
//        googleUserInfo.checkNullProviderId();
//
//        // 2. 내부 DB와 관련된 비지니스 로직은 IdentityService에게 위임
//        identityService.linkSocialAccount(
//                userAuthInfo.getUserId(),
//                provider,
//                googleUserInfo.getProviderId()
//        );
//        return new OAuthLinkResponse("연동완료", provider);
//    }

    // 2. Refresh 토큰 발급
    public LoginResponse refresh(String refreshToken) {
        // 1. Refresh 토큰 형식 검사(JWT 형식검사)
        verifyTokenStructure(refreshToken);

        // 2. refresh 토큰에서 identityId 추출
        Long identityId = tokenProvider.extractIdentityId(refreshToken);

        // 3. Redis 저장값과 비교(Token Rotation 체크 - 탈취 방어)

        log.info("Refresh Token: {}", refreshToken.toString());
        tokenStoragePort.verifyTokenMatch(identityId, refreshToken);

        // 4. 실시간 유저 상태확인
        Identity identity = identityService.getActiveIdentity(identityId);

        // 5. 새로운 토큰 발급
        TokenResponse newTokens = tokenProvider.rotateToken(identity.toAuthInfo(), refreshToken);

        // 6. Redis 인프라 갱신
        updateStoredToken(identityId, newTokens.getRefreshToken(), refreshToken);

        // 7. LoginResponse 생성
        LoginResponse response = LoginResponse.builder()
                .refreshToken(newTokens.getRefreshToken())
                .refreshTokenDuration(newTokens.getRefreshTokenDuration())
                .accessToken(newTokens.getAccessToken())
                .email(identity.getEmail())
                .userName(userProfilePort.getUserFullName(identityId))
                .role(identity.getRole())
                .userId(identityId)
                .build();

        return response;
    }

    // 3. Oauth 로그인 후 성공 처리
    public OAuthLoginResponse processOAuthLogin(CustomOAuth2User oAuth2User) {
        // 1. 넘어온 정보 추출
        AuthProvider provider = oAuth2User.getProvider(); // GOOGLE
        String providerId = oAuth2User.getProviderId(); // 1234567..
        String email = (String) oAuth2User.getAttributes().get("email"); // 구글에서 넘어온 이메일

        // 1. 소셜로 가입한 사람의 아이디를 조회
        return identityService.findAuthInfoBySocial(provider, providerId)
                .map(authInfo -> {
                    // 가입된 유저 -> 토큰 발급 후 메인으로 리다이렉트
                    TokenResponse tokens = tokenProvider.issueToken(authInfo);
                    tokenStoragePort.updateRefreshToken(
                            authInfo.getUserId(),
                            tokens.getRefreshToken(),
                            tokens.getRefreshTokenDuration()
                    );

                    String finalUrl = UriComponentsBuilder.fromUriString((successRedirectUri))
                            .queryParam("token", tokens.getAccessToken())
                            .build()
                            .toUriString();

                    return new OAuthLoginResponse(false, finalUrl, authInfo);
                })
                .orElseGet(() -> {
                            // 미가입 유저 -> 회원가입 페이지로 리다이렉트
                            String signUpUrl = UriComponentsBuilder.fromUriString(signupRedirectUri)
                                    .queryParam("provider", provider.name())
                                    .queryParam("providerId", providerId)
                                    .queryParam("email", email)
                                    .build()
                                    .toUriString();
                            return new OAuthLoginResponse(true, signUpUrl, null);
                        }
                );
    }

    // 4. OAuth 로그인 실패 후 리다이렉트
    /*
        1. 사용자 취소
        2. 서버 통신 오류
        3. 설정 오류(clientId, redirectUri 설정이 구글 개발자 콘솔과 다를때)
        4. 구글이 준 code가 너무 오래되서 요청 만료
     */
    public String processOAuthFailure(Exception exception) {
        return UriComponentsBuilder.fromUriString(failureRedirectUrl)
                .queryParam("error", "oauth_authentication_failed")
                .queryParam("message", exception.getMessage())
                .build()
                .toUriString();
    }

    // 5. 로그아웃
    @Transactional
    public void logout(String refreshToken) {
        // 1. 토큰이 유효하지 않으면 예외 발생
        tokenProvider.validateToken(refreshToken);

        // 3. 토큰에서 userId 추출
        Long userId = tokenProvider.extractIdentityId(refreshToken);

        // 3. refresh 삭제
        tokenStoragePort.deleteRefreshToken(userId);
    }

    // 토큰 형식 검사(JWT 형식검사)
    private void verifyTokenStructure(String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new IdentityException(IdentityErrorCode.INVALID_TOKEN);
        }
    }

    // -- Refresh 토큰 발급 헬퍼
    private void updateStoredToken(Long identityId, String newRefreshToken, String oldRefreshToken) {
        long ttl = tokenProvider.getRemainingMillis(oldRefreshToken);
        tokenStoragePort.updateRefreshToken(identityId, newRefreshToken, ttl);
    }


}
/*
 *  [중요중요]
 *  IdentityService: 회원이 맞는지? / 도메인지식
 *  TokenProvider: 토큰 발급 및 시간계산. / 기술지식
 *  RedisTool: 토큰 저장. / 인프라 지식
 *  AuthService: 신분 확인 하고 토큰 발급하고 토큰 전달. / 시나리오 조율
 */
