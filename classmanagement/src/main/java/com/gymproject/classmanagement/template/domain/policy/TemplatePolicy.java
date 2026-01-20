package com.gymproject.classmanagement.template.domain.policy;

import com.gymproject.classmanagement.template.domain.type.ClassKind;
import com.gymproject.classmanagement.template.exception.TemplateErrorCode;
import com.gymproject.classmanagement.template.exception.TemplateException;

public class TemplatePolicy {

    private static final int MIN_CAPACITY = 1; // 최소 1명 이상

    public static void validate(int capacity, int duration, ClassKind classKind){
        if(capacity < MIN_CAPACITY) throw new TemplateException(TemplateErrorCode.INVALID_CAPACITY);
        if(classKind == ClassKind.PERSONAL && capacity != 1){
            throw new TemplateException(TemplateErrorCode.INVALID_PERSONAL_CAPACITY);
        }
        if(classKind == ClassKind.GROUP && capacity <= 1){
            throw new TemplateException(TemplateErrorCode.INVALID_GROUP_CAPACITY);
        }
        if(duration % 10 != 0){
            throw new TemplateException(TemplateErrorCode.INVALID_DURATION_UNIT);
        }
    }


}
