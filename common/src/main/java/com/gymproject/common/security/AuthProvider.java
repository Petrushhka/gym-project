package com.gymproject.common.security;

import com.gymproject.common.exception.auth.UnsupportedRegistrationIdException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AuthProvider {
    LOCAL(null, null),
    GOOGLE("google", "sub"),
    APPLE("apple", "sub");

    private final String provider; // OAuth2 provider 이름
    private final String providerCode; // provider에서 사용한 고유 식별자 key

    public static AuthProvider fromRegistrationId(String regId) {
        if(regId == null || regId.isBlank()) {
            throw new UnsupportedRegistrationIdException("registrationId 값 확인(null/blank)");
        }
        if (regId.startsWith("google")) return AuthProvider.GOOGLE;
        if (regId.startsWith("apple")) return AuthProvider.APPLE;

        throw new UnsupportedRegistrationIdException("지원하지 않는 registrationId: " + regId);
    }
}

/*
    OAtuh2 응답 구조에 맞게 미리 맵핑해놓은 타입임

e.g.) 구글 로그인 성공시 응답 Json - 실제가 아니라 예시임
{
  "sub": "112833493884934834",     //  구글 내부에서 유저를 구분하는 고유 ID
  "email": "gildong@gmail.com",    //  사용자의 이메일
  "email_verified": true,
  "name": "gildong hong",
  "picture": "https://lh3.googleusercontent.com/a/...photo.jpg"
}
e.g.) 애플 로그인 성공시 응답 Json - 실제가 아니라 예시임
{
  "sub": "000832.abcdef.1298ff0d", //  애플의 유저 고유 ID
  "email": "gildong@icloud.com"     //  사용자의 이메일
}


 */