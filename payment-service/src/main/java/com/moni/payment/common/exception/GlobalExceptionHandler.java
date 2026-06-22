package com.moni.payment.common.exception;

import com.moni.common.error.exception.CustomException;
import com.moni.common.error.exception.ErrorCode;
import com.moni.common.error.exception.ExceptionHandlerSupport;
import com.moni.common.response.GlobalResponse;
import com.moni.payment.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
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
