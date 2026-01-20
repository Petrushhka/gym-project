package com.gymproject.api.exception;

import com.gymproject.common.dto.exception.CommonResDto;
import com.gymproject.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice("com.gymproject")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 1. 비즈니스 예외 통합 처리
     * 모든 커스텀 예외(InvalidRole, DuplicateUser 등) 하나로 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResDto<?>> handleBusinessException(BusinessException ex) {
        log.warn("Business Error [{}]: {}", ex.getErrorCode(), ex.getMessage());

        // 사용자에게 보여줄 메세지 결정
        String displayMessage;

        if (ex.getErrorCode().equalsIgnoreCase("BAD_REQUEST") ||
                ex.getErrorCode().startsWith("INTERNAL_")) {
            displayMessage = "요청 정보가 올바르지 않습니다. 다시 시도해주세요.";
        } else {
            displayMessage = ex.getMessage();
        }

        return ResponseEntity.status(ex.getStatusCode())
                .body(CommonResDto.error(
                        ex.getStatusCode(),
                        ex.getErrorCode(),
                        displayMessage
                ));
    }

    /**
     * 2.
     * DTO에서 설정한 Message를 꺼내서 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResDto<?>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("유효성 검사 실패: {}", ex.getMessage());

        BindingResult bindingResult = ex.getBindingResult(); // 에러더미에서 첫번째 메시지만 꺼냄
        String firstErrorMessage = "입력값이 잘못되었습니다."; // 기본값

        if(bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            // dto에서 적은 message가 나옴 "비밀번호는 필수입니다."
            if(fieldError != null) {
                firstErrorMessage = fieldError.getDefaultMessage();
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CommonResDto.error(
                        HttpStatus.BAD_REQUEST.value(),
                        "BAD_REQUEST",
                        firstErrorMessage
                ));
    }

    /**
     * 3. 처리되지 않은 시스템 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResDto<?>> handleGeneral(Exception ex) {
        log.error("Unhandled exception 발생", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResDto.error(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "INTERNAL_SERVER_ERROR",
                        "서버 내부 오류가 발생했습니다."
                ));
    }
}



/*
    원래 Map<<String,String> 형식으로 에러코드와 에러메세지를 같이 보냇는데
    common 패키지에 ErrorResponseDto를 만들어서 그 객체를 응답하게끔했음.
    근데 그걸 다시 CommonResDto로 바꿈.
 */
