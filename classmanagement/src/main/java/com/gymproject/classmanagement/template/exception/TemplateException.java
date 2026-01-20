package com.gymproject.classmanagement.template.exception;

import com.gymproject.common.exception.BusinessException;

public class TemplateException extends BusinessException {

    // 에러 코드만 넘길 때
    public TemplateException(TemplateErrorCode errorCode) {
        super(errorCode.getMessage(), errorCode.getStatusCode(),  errorCode.getErrorCode());
    }

    public TemplateException(TemplateErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage() + " (상세: %s)", args),
                errorCode.getStatusCode(),
                errorCode.getErrorCode());
    }
}
