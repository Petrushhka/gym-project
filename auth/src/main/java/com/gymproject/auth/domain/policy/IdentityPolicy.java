package com.gymproject.auth.domain.policy;

import com.gymproject.auth.domain.entity.Oauth;
import com.gymproject.auth.exception.IdentityErrorCode;
import com.gymproject.auth.exception.IdentityException;
import com.gymproject.common.event.domain.ProfileInfo;
import com.gymproject.common.security.AuthProvider;

import java.util.List;
import java.util.regex.Pattern;

public final class IdentityPolicy {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public static void validateEmail(String email) {
        if(email == null || email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IdentityException(IdentityErrorCode.INVALID_EMAIL);
        }
    }

    public static void validateProfile(ProfileInfo profile){
        if(profile == null){
            throw new IdentityException(IdentityErrorCode.INVALID_PARAM);
        }
     }

    public  static void validateLinked(List<Oauth> oauths, AuthProvider provider){
         boolean isLinked = oauths.stream()
                 .anyMatch(oauth -> oauth.getOauthProvider() == provider);
         if(isLinked) {
             throw new IdentityException(IdentityErrorCode.ALREADY_LINKED);
         }
     }
}
