package com.moni.portfolio.global.exception;

import com.moni.common.error.exception.CustomException;
import com.moni.common.error.exception.ExceptionHandlerSupport;
import com.moni.common.response.GlobalResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<Void>> handleException(Exception e) {
        return ExceptionHandlerSupport.handle(e);
    }
}
