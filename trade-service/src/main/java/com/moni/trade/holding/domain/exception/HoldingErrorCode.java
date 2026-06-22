package com.moni.trade.holding.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HoldingErrorCode implements ErrorCode {

    HOLDING_NOT_FOUND("HOLDING_001", "보유 종목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INSUFFICIENT_QUANTITY("HOLDING_002", "보유 수량이 부족합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
