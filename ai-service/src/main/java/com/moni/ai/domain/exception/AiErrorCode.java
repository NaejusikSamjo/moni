package com.moni.ai.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {
    TICKER_NOT_FOUND("AI-001", "분석되지 않는 ticker입니다.",HttpStatus.NOT_FOUND),
    AI_NOT_FOUND("AI-002", "분석 결과가 없습니다. POST로 먼저 분석을 요청해주세요.",HttpStatus.NOT_FOUND),
    ANALYSIS_ALREADY_EXISTS("AI-003", "이미 분석된 기업입니다.", HttpStatus.BAD_REQUEST);
    ;

    private final String code;
    private final String message;
    private final HttpStatus status;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }
}
