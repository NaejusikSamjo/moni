package com.moni.portfolio.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PortfolioErrorCode implements ErrorCode {

    PORTFOLIO_NOT_FOUND("PORTFOLIO-001", "포트폴리오를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    HOLDING_NOT_FOUND("PORTFOLIO-002", "보유 종목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    STOCK_PRICE_NOT_FOUND("PORTFOLIO-003", "현재가 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    EXTERNAL_SERVICE_ERROR("PORTFOLIO-004", "외부 서비스 연동 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    INVALID_PORTFOLIO_QUERY("PORTFOLIO-005", "잘못된 포트폴리오 조회 요청입니다.", HttpStatus.BAD_REQUEST);

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
