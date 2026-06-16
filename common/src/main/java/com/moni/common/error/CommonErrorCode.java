package com.moni.common.error;

import com.moni.common.error.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    UNAUTHORIZED("AUTH_001", "인증 정보가 없습니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("AUTH_002", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
