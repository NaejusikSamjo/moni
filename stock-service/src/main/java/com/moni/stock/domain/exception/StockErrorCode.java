package com.moni.stock.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StockErrorCode implements ErrorCode {

    STOCK_NOT_FOUND("S001", "종목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    KIS_CONNECTION_FAILED("S002", "한국투자증권 연결에 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String message;
    private final HttpStatus status;
}