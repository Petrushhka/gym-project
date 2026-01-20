package com.gymproject.user.profile.domain.policy;

import com.gymproject.user.profile.exception.UserErrorCode;
import com.gymproject.user.profile.exception.UserException;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public final class UserProfilePolicy {

    // 이름: 영어 대소문자, 공백, 하이픈, 어퍼스트로피 허용
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-']+$");

    public static void validateRegisterName(String firstName, String lastName){
        validateNamePart(firstName, "First Name");
        validateNamePart(lastName, "Last Name");
    }

    public static void validateUpdateName(String firstName, String lastName){
        if(firstName != null) validateNamePart(firstName, "First Name");
        if(lastName != null) validateNamePart(lastName, "Last Name");
    }

    private static void validateNamePart(String name, String field){
        if(!StringUtils.hasText(name)){
            throw new UserException(UserErrorCode.INVALID_NAME_FORMAT, field + "is required");
        }
        if(name.length()>50){
            throw new UserException(UserErrorCode.INVALID_NAME_FORMAT, field + "is too long (max 50 chars");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new UserException(UserErrorCode.INVALID_NAME_FORMAT, field + " contains invalid characters.");
        }
    }

}
