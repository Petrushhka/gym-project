package com.gymproject.user.sesssion.domain.type;

import com.gymproject.user.sesssion.exception.UserSessionErrorCode;
import com.gymproject.user.sesssion.exception.UserSessionsException;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Arrays;

@Getter
public enum SessionProductType {
    PT_10("PT_10", 10, 90), // 10회
    PT_20("PT_20", 20, 120), // 20회
    PT_30("PT_30", 30, 180); // 30회

    private final String code;
    private final int sessionCount;
    private final int durationDays;

    SessionProductType(String code, int sessionCount, int durationDays) {
        this.code = code;
        this.sessionCount = sessionCount;
        this.durationDays = durationDays;
    }

    // 세션권 종료 기간
    public OffsetDateTime calculateExpiredAt(OffsetDateTime now){
        return now.plusDays(durationDays);
    }

    public static SessionProductType findByCode(String code){
        return Arrays.stream(SessionProductType.values())
                .filter(type-> type.code.equals(code))
                .findFirst()
                .orElseThrow(()-> new UserSessionsException(UserSessionErrorCode.INVALID_PRODUCT_TYPE));
    }


}