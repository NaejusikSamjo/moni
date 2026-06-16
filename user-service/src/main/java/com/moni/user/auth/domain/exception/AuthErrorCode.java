package com.moni.user.auth.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    UNAUTHORIZED("AUTH-001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("AUTH-002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    BLACKLISTED_TOKEN("AUTH-003", "이미 로그아웃된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_TYPE_MISMATCH("AUTH-004", "토큰 타입이 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_PASSWORD("AUTH-005", "비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),

    FORBIDDEN("AUTH-006", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    USER_SUSPENDED("AUTH-007", "정지된 계정입니다.", HttpStatus.FORBIDDEN),
    USER_DELETED("AUTH-008", "탈퇴한 계정입니다.", HttpStatus.FORBIDDEN),

    USER_NOT_FOUND("AUTH-009", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),

    EMAIL_DUPLICATE("AUTH-010", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    OAUTH_PROVIDER_MISMATCH("AUTH-011", "해당 이메일은 다른 소셜 로그인으로 가입되어 있습니다.", HttpStatus.CONFLICT);

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
