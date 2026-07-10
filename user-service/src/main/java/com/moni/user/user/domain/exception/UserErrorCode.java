package com.moni.user.user.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("USER-001", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    TENDENCY_NOT_FOUND("USER-002", "투자 성향 정보가 없습니다.", HttpStatus.NOT_FOUND),
    WATCHLIST_NOT_FOUND("USER-003", "관심종목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    NICKNAME_DUPLICATE("USER-004", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    TENDENCY_ALREADY_EXISTS("USER-005", "이미 투자 성향이 등록되어 있습니다.", HttpStatus.CONFLICT),
    WATCHLIST_ALREADY_EXISTS("USER-006", "이미 관심종목에 추가된 종목입니다.", HttpStatus.CONFLICT),

    PASSWORD_SAME_AS_CURRENT("USER-007", "현재 비밀번호와 동일합니다.", HttpStatus.BAD_REQUEST),
    PASSWORD_WRONG("USER-011", "현재 비밀번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),

    FORBIDDEN("USER-008", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    USER_ALREADY_SUSPENDED("USER-009", "이미 정지된 계정입니다.", HttpStatus.CONFLICT),
    USER_NOT_SUSPENDED("USER-010", "정지 상태가 아닌 계정입니다.", HttpStatus.BAD_REQUEST),

    S3_PRESIGNED_URL_FAILED("USER-012", "프로필 업로드 URL 생성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    ALREADY_INTEGRATED("USER-013", "이미 통합 회원으로 전환된 계정입니다.", HttpStatus.CONFLICT),
    NOT_OAUTH_ACCOUNT("USER-015", "소셜 로그인으로 가입된 계정이 아닙니다.", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED("USER-014", "비밀번호 확인이 필요합니다.", HttpStatus.BAD_REQUEST);

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