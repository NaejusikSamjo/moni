package com.moni.trade.account.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AccountErrorCode implements ErrorCode {

    ACCOUNT_NOT_FOUND("ACCOUNT_001", "계좌를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ACCOUNT_ALREADY_EXISTS("ACCOUNT_002", "이미 계좌가 존재합니다.", HttpStatus.CONFLICT),
    INSUFFICIENT_BALANCE("ACCOUNT_003", "잔액이 부족합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
