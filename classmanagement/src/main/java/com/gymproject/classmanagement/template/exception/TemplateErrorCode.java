package com.gymproject.classmanagement.template.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TemplateErrorCode {

    // 404: Not Found
    NOT_FOUND("해당 수업 템플릿을 찾을 수 없습니다.", 404, "TEMPLATE_404"),

    // 400: Business Logic Validation
    ALREADY_DELETED("이미 삭제된 템플릿입니다.", 400, "TEMPLATE_ALREADY_DELETED"),

    // Policy Validation
    INVALID_TITLE("수업 제목은 필수이며, 비워둘 수 없습니다.", 400, "TEMPLATE_INVALID_TITLE"),
    INVALID_CAPACITY("정원은 최소 1명 이상이어야 합니다.", 400, "TEMPLATE_INVALID_CAPACITY"),
    INVALID_PERSONAL_CAPACITY("개인 수업(1:1)의 정원은 반드시 1명이어야 합니다.", 400, "TEMPLATE_INVALID_PERSONAL_CAPACITY"),
    INVALID_GROUP_CAPACITY("그룹 수업의 정원은 최소 2명 이상이어야 합니다.", 400, "TEMPLATE_INVALID_GROUP_CAPACITY"),
    INVALID_DURATION_UNIT("수업 시간은 10분 단위로만 설정 가능합니다.", 400, "TEMPLATE_INVALID_DURATION_UNIT");

    private final String message;
    private final int statusCode;
    private final String errorCode;
}
