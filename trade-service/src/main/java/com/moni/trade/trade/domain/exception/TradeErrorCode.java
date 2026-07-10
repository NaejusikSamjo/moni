package com.moni.trade.trade.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TradeErrorCode implements ErrorCode {

    TRADE_NOT_FOUND("TRADE_001", "거래 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    STOCK_PRICE_FETCH_FAILED("TRADE_002", "현재가 조회에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    INSUFFICIENT_AMOUNT("TRADE_003", "최소 0.01주 이상 구매 가능한 금액이 필요합니다.", HttpStatus.BAD_REQUEST),
    TRADE_CONCURRENT_CONFLICT("TRADE_004", "동시 요청 충돌이 발생했습니다. 다시 시도해 주세요.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
