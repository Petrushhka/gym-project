package com.gymproject.support.email;

public class EmailMasker {
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            // email이 없는 유저는 소셜가입자
            throw new IllegalArgumentException("이메일 값이 비어있습니다.");
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) {
            return email.charAt(0) + "***" + email.substring(atIndex);
        }

        return email.substring(0, 3) + "***" + email.substring(atIndex);
    }
}
