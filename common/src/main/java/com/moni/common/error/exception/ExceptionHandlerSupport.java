package com.moni.common.error.exception;

import com.moni.common.error.ErrorResponse;
import com.moni.common.response.GlobalResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashMap;
import java.util.Map;

public final class ExceptionHandlerSupport {

    private ExceptionHandlerSupport() {
    }

    public static ResponseEntity<GlobalResponse<Void>> handle(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse errorResponse = ErrorResponse.of(errorCode.getCode(), e.getMessage());
        GlobalResponse<Void> response = GlobalResponse.failure(
                errorCode.getStatus().value(), errorCode.getCode(), errorResponse);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    public static ResponseEntity<GlobalResponse<Void>> handle(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ErrorResponse errorResponse = ErrorResponse.of("VALIDATION_ERROR", errors.toString());
        GlobalResponse<Void> response = GlobalResponse.failure(
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.name(), errorResponse);
        return ResponseEntity.badRequest().body(response);
    }

    public static ResponseEntity<GlobalResponse<Void>> handle(Exception e) {
        ErrorResponse errorResponse = ErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");
        GlobalResponse<Void> response = GlobalResponse.failure(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.name(), errorResponse);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
