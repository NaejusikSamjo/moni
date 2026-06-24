package com.moni.portfolio.presentation.controller;

import com.moni.common.response.GlobalResponse;
import com.moni.portfolio.application.service.PortfolioService;
import com.moni.portfolio.presentation.dto.response.PortfolioAssetResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioCreateResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioHoldingsResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Portfolio", description = "포트폴리오 생성, 자산 및 보유 종목 API")
@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String DEFAULT_SORT = "evaluationAmount,desc";

    private final PortfolioService portfolioService;

    @Operation(summary = "포트폴리오 생성", description = "현재 사용자의 포트폴리오를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "포트폴리오 생성 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청"),
            @ApiResponse(responseCode = "409", description = "PORTFOLIO-004: 이미 포트폴리오가 존재함")
    })
    @PostMapping
    public ResponseEntity<GlobalResponse<PortfolioCreateResponseDto>> createPortfolio(
            @Parameter(hidden = true)
            @RequestHeader(HEADER_USER_ID) UUID userId
    ) {
        PortfolioCreateResponseDto response = portfolioService.createPortfolio(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(HttpStatus.CREATED.value(), response));
    }

    @Operation(summary = "자산 조회", description = "예수금과 보유 종목 평가 결과를 합산한 자산 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "자산 조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청"),
            @ApiResponse(responseCode = "404", description = "PORTFOLIO-002: 포트폴리오 없음 / PORTFOLIO-003: 현재가 없음 / PORTFOLIO-005: 투자 계좌 없음"),
            @ApiResponse(responseCode = "502", description = "PORTFOLIO-101~102: 외부 응답 오류 / PORTFOLIO-901~902: 외부 서비스 연동 오류"),
            @ApiResponse(responseCode = "504", description = "PORTFOLIO-903~904: 외부 서비스 응답 시간 초과")
    })
    @GetMapping("/assets")
    public ResponseEntity<GlobalResponse<PortfolioAssetResponseDto>> getAssets(
            @Parameter(hidden = true)
            @RequestHeader(HEADER_USER_ID) UUID userId
    ) {
        PortfolioAssetResponseDto response = portfolioService.getAssets(userId);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }

    @Operation(summary = "보유 종목 현황 조회", description = "보유 종목별 평가금액, 손익, 수익률과 포트폴리오 비중을 페이지로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보유 종목 현황 조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청"),
            @ApiResponse(responseCode = "404", description = "PORTFOLIO-002: 포트폴리오 없음 / PORTFOLIO-003: 현재가 없음 / PORTFOLIO-005: 투자 계좌 없음"),
            @ApiResponse(responseCode = "502", description = "PORTFOLIO-101~102: 외부 응답 오류 / PORTFOLIO-901~902: 외부 서비스 연동 오류"),
            @ApiResponse(responseCode = "504", description = "PORTFOLIO-903~904: 외부 서비스 응답 시간 초과")
    })
    @GetMapping("/holdings")
    public ResponseEntity<GlobalResponse<PortfolioHoldingsResponseDto>> getHoldings(
            @Parameter(hidden = true)
            @RequestHeader(HEADER_USER_ID) UUID userId,
            @Parameter(description = "페이지 번호이며 음수는 0으로 보정됩니다.", schema = @Schema(minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기이며 1부터 50 이외의 값은 10으로 보정됩니다.", schema = @Schema(minimum = "1", maximum = "50"))
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "평가금액 정렬 조건", schema = @Schema(allowableValues = {"evaluationAmount,desc", "evaluationAmount,asc"}))
            @RequestParam(defaultValue = DEFAULT_SORT) String sort
    ) {
        PortfolioHoldingsResponseDto response = portfolioService.getHoldings(userId, page, size, sort);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }
}
