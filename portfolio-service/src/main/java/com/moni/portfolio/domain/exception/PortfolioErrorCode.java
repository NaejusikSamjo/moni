package com.moni.portfolio.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PortfolioErrorCode implements ErrorCode {

    INVALID_PORTFOLIO_QUERY("PORTFOLIO-001", "잘못된 포트폴리오 조회 요청입니다.", HttpStatus.BAD_REQUEST),
    PORTFOLIO_NOT_FOUND("PORTFOLIO-002", "포트폴리오를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    STOCK_PRICE_NOT_FOUND("PORTFOLIO-003", "현재가 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PORTFOLIO_ALREADY_EXISTS("PORTFOLIO-004", "이미 포트폴리오가 존재합니다.", HttpStatus.CONFLICT),
    TRADE_ACCOUNT_NOT_FOUND("PORTFOLIO-005", "투자 계좌를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PORTFOLIO_ANALYSIS_NOT_FOUND("PORTFOLIO-006", "포트폴리오 AI 분석 결과를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    TRADE_RESPONSE_INVALID("PORTFOLIO-101", "Trade 서비스 응답 데이터가 올바르지 않습니다.", HttpStatus.BAD_GATEWAY),
    STOCK_RESPONSE_INVALID("PORTFOLIO-102", "Stock 서비스 응답 데이터가 올바르지 않습니다.", HttpStatus.BAD_GATEWAY),
    USER_TENDENCY_RESPONSE_INVALID("PORTFOLIO-103", "User 서비스 투자 성향 응답 데이터가 올바르지 않습니다.", HttpStatus.BAD_GATEWAY),
    AI_RESPONSE_INVALID("PORTFOLIO-104", "AI 서비스 응답 데이터가 올바르지 않습니다.", HttpStatus.BAD_GATEWAY),

    TRADE_SERVICE_ERROR("PORTFOLIO-901", "Trade 서비스 연동 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    TRADE_SERVICE_TIMEOUT("PORTFOLIO-902", "Trade 서비스 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    STOCK_SERVICE_ERROR("PORTFOLIO-903", "Stock 서비스 연동 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    STOCK_SERVICE_TIMEOUT("PORTFOLIO-904", "Stock 서비스 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    USER_SERVICE_ERROR("PORTFOLIO-905", "User 서비스 연동 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    USER_SERVICE_TIMEOUT("PORTFOLIO-906", "User 서비스 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    AI_SERVICE_ERROR("PORTFOLIO-907", "AI 서비스 연동 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    AI_SERVICE_TIMEOUT("PORTFOLIO-908", "AI 서비스 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
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
