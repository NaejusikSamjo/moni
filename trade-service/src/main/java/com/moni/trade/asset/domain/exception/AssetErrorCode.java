package com.moni.trade.asset.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AssetErrorCode implements ErrorCode {

    STOCK_PRICE_NOT_FOUND("ASSET-001", "현재가 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    STOCK_RESPONSE_INVALID("ASSET-101", "Stock 서비스 응답 데이터가 올바르지 않습니다.", HttpStatus.BAD_GATEWAY),

    STOCK_SERVICE_ERROR("ASSET-901", "Stock 서비스 연동 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    STOCK_SERVICE_TIMEOUT("ASSET-902", "Stock 서비스 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
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
