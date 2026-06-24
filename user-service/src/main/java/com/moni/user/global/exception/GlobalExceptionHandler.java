package com.moni.user.global.exception;

import com.moni.common.error.exception.CustomException;
import com.moni.common.error.exception.ExceptionHandlerSupport;
import com.moni.common.response.GlobalResponse;
import com.moni.common.error.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<GlobalResponse<Void>> handleCustomException(CustomException e) {
        return ExceptionHandlerSupport.handle(e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        return ExceptionHandlerSupport.handle(e);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GlobalResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        ErrorResponse errorResponse = ErrorResponse.of("VALIDATION_ERROR", message);
        return ResponseEntity.badRequest().body(
                GlobalResponse.failure(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<Void>> handleException(Exception e) {
        return ExceptionHandlerSupport.handle(e);
    }
}
