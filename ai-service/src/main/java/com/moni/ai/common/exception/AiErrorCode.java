package com.moni.ai.common.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;

    @Override
    public String getCode() {
        return "";
    }

    @Override
    public String getMessage() {
        return "";
    }

    @Override
    public HttpStatus getStatus() {
        return null;
    }
}
