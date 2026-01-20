package com.gymproject.user.sesssion.exception;

import com.gymproject.common.exception.BusinessException;

public class UserSessionsException extends BusinessException {

    public UserSessionsException(UserSessionErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatusCode(), errorCode.getErrorCode());
    }

    public UserSessionsException(UserSessionErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage() + "(상세: %s", args), // eg. 만료된 세션권입니다. 상세: args
                errorCode.getStatusCode(),
                errorCode.getErrorCode());
    }
}

/* [중요] 가변인자
    Object... (0 개 ~ N개)
    "파라미터의 개수를 정하지 않고 마음대로 넣겠다."
    파라미터들은 Object[] 로 취급됨.

    예외마다 필요한 정보의 개수가 다름.
    1) 세션이 만료되었습니다.
    2) 세션이 만료되었습니다. ID: 10
    3) 세션이 만료되었습니다. ID: 10, 만료일: 2020-01-01

    String.forma("%s") 로 처리해서 Object타입을 알아서 String으로 형변환해서 보여줌.

 */