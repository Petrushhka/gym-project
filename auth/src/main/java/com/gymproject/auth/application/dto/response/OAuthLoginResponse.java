package com.gymproject.auth.application.dto.response;

import com.gymproject.common.dto.auth.UserAuthInfo;

public record OAuthLoginResponse(
        boolean isNewUser,
        String redirectUrl,
        UserAuthInfo userAuthInfo
) {
}
