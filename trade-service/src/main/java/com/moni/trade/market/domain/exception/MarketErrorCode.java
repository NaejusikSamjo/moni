package com.moni.trade.market.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MarketErrorCode implements ErrorCode {

    MARKET_CLOSED("MARKET_001", "현재 거래 가능한 시간이 아닙니다. (평일 09:00 ~ 15:30)", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
