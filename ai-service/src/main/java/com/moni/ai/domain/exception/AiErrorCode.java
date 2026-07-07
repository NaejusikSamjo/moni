package com.moni.ai.domain.exception;

import com.moni.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {
    TICKER_NOT_FOUND("AI-001", "분석되지 않는 ticker입니다.",HttpStatus.NOT_FOUND),
    AI_NOT_FOUND("AI-002", "분석 결과가 없습니다. POST로 먼저 분석을 요청해주세요.",HttpStatus.NOT_FOUND),
    COMPANY_NAME_MISMATCH("AI-003", "ticker와 회사명이 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    NEWS_ALREADY_EXISTS("AI-004", "이미 등록된 뉴스입니다.", HttpStatus.CONFLICT),
    ANALYSIS_ALREADY_EXISTS("AI-005", "이미 분석됐습니다. GET으로 분석을 반환하세요.", HttpStatus.CONFLICT),
    PORTFOLIO_ANALYSIS_REQUEST_INVALID("AI-006", "포트폴리오 분석 요청값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    PORTFOLIO_LLM_TIMEOUT("AI-007", "포트폴리오 분석 LLM 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    PORTFOLIO_LLM_PROVIDER_FAILED("AI-008", "포트폴리오 분석 LLM 호출 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    PORTFOLIO_LLM_RESPONSE_INVALID("AI-009", "포트폴리오 분석 LLM 응답 구조가 올바르지 않습니다.", HttpStatus.BAD_GATEWAY),
    AI_RESPONSE_FAILED("AI-010", "AI 응답이 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    MARKET_KEYWORD_MISMATCH("AI-011","분석되지 않는 시장 키워드 입니다.", HttpStatus.NOT_FOUND),
    ANALYSIS_IN_PROGRESS("AI-012", "분석이 진행 중입니다. 잠시 후 다시 시도해주세요.", HttpStatus.CONFLICT);
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
