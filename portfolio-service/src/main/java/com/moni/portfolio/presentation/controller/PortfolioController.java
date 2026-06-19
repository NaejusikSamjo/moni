package com.moni.portfolio.presentation.controller;

import com.moni.common.response.GlobalResponse;
import com.moni.portfolio.application.service.PortfolioService;
import com.moni.portfolio.presentation.dto.response.PortfolioAssetResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioHoldingProfitLossResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioHoldingsResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioReturnsResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Portfolio", description = "포트폴리오 API")
@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String DEFAULT_SORT = "evaluationAmount,desc";

    private final PortfolioService portfolioService;

    @Operation(summary = "자산 조회")
    @GetMapping("/assets")
    public ResponseEntity<GlobalResponse<PortfolioAssetResponseDto>> getAssets(
            @RequestHeader(HEADER_USER_ID) UUID userId
    ) {
        PortfolioAssetResponseDto response = portfolioService.getAssets(userId);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }

    @Operation(summary = "보유 종목 현황 조회")
    @GetMapping("/holdings")
    public ResponseEntity<GlobalResponse<PortfolioHoldingsResponseDto>> getHoldings(
            @RequestHeader(HEADER_USER_ID) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = DEFAULT_SORT) String sort
    ) {
        PortfolioHoldingsResponseDto response = portfolioService.getHoldings(userId, page, size, sort);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }

    @Operation(summary = "종목별 손익 조회")
    @GetMapping("/holdings/{ticker}/profit-loss")
    public ResponseEntity<GlobalResponse<PortfolioHoldingProfitLossResponseDto>> getHoldingProfitLoss(
            @RequestHeader(HEADER_USER_ID) UUID userId,
            @PathVariable String ticker
    ) {
        PortfolioHoldingProfitLossResponseDto response = portfolioService.getHoldingProfitLoss(userId, ticker);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }

    @Operation(summary = "수익률 계산 및 조회")
    @GetMapping("/returns")
    public ResponseEntity<GlobalResponse<PortfolioReturnsResponseDto>> getReturns(
            @RequestHeader(HEADER_USER_ID) UUID userId
    ) {
        PortfolioReturnsResponseDto response = portfolioService.getReturns(userId);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }
}
