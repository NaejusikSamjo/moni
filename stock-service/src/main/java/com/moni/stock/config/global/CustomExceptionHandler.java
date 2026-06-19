package com.moni.stock.config.global;

import com.moni.common.error.ErrorResponse;
import com.moni.common.error.exception.CustomException;
import com.moni.common.error.exception.ErrorCode;
import com.moni.common.response.GlobalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
class CustomExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<GlobalResponse> handleCustomException(CustomException e) {
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponse errorResponse = ErrorResponse.of(errorCode.getCode(), e.getMessage());
        final GlobalResponse response = GlobalResponse.failure(errorCode.getStatus().value(), errorCode.getCode() ,errorResponse);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponse errorResponse = ErrorResponse.of("VALIDATION_ERROR", errors.toString());
        GlobalResponse response = GlobalResponse.failure(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.name(), errorResponse);
        return ResponseEntity.badRequest().body(response);
    }


}