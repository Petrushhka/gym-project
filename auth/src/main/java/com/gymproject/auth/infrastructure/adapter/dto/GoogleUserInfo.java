package com.gymproject.auth.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gymproject.auth.exception.OauthErrorCode;
import com.gymproject.auth.exception.OauthException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleUserInfo {
    @JsonProperty("sub") // 구글이 보내는 고유 번호
    private String providerId;

    public void checkNullProviderId(){
        if(this.providerId == null || this.providerId.isBlank()){
            throw new OauthException(OauthErrorCode.INVALID_EXTERNAL_RESPONSE);
        }
    }
}
