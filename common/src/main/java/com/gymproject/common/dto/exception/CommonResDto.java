package com.gymproject.common.dto.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CommonResDto<T> {
    @Schema(description = "HTTP 상태 코드", example = "200")
    private int status;

    @Schema(
            description = "에러 코드 (성공 시 null, 실패 시 커스텀 에러 코드)",
            example = "null"  // 성공 예시에서는 null로 보이게 설정
    )
    private String errorCode; // [중요] 코드를 넣어야하는 이유: 프론트엔드 분기 처리, 로그에 숫자가 아닌 문자코드가 찍히는게 직관적

    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
    private String message;

    @Schema(description = "실제 데이터 (Payload)")
    private T data;

    // 성공 - 데이터만 넣으면 기본 200 OK 지정
    public static<T> CommonResDto<T> success(T data) {
        return new CommonResDto<>(200, null, "요청이 성공적으로 처리되었습니다.", data);
    }

    // 성공 - 메시지와 데이터를 직접 지정
    public static <T> CommonResDto<T> success(int status, String message, T data) {
        return new CommonResDto<>(status,null, message, data);
    }

    // 에러 - 상태코드와 메시지 지정
    public static <T> CommonResDto<T> error(int status, String errorCode, String message) {
        return new CommonResDto<>(status, errorCode, message, null);
    }

}

/*
    모든 API 응답의 공통 규격을 담는 DTO
    프론트엔드와 약속한 통신 프로토콜
 */