package com.moni.portfolio.presentation.controller;

import com.moni.common.response.GlobalResponse;
import com.moni.common.response.paging.PageRes;
import com.moni.portfolio.application.service.PortfolioAnalysisService;
import com.moni.portfolio.presentation.dto.response.PortfolioAnalysisCreateResponseDto;
import com.moni.portfolio.presentation.dto.response.PortfolioAnalysisResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Portfolio AI Analysis", description = "포트폴리오 AI 분석 API")
@RestController
@RequestMapping("/api/v1/portfolio/ai-analysis")
@RequiredArgsConstructor
public class PortfolioAnalysisController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final PortfolioAnalysisService portfolioAnalysisService;

    @Operation(
            summary = "포트폴리오 AI 분석 요청",
            description = "사용자의 포트폴리오 스냅샷과 투자 성향 적합도를 기반으로 AI 분석을 요청합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "분석 요청 접수", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "PORTFOLIO-002: 포트폴리오 없음"),
            @ApiResponse(responseCode = "502", description = "외부 서비스 응답 오류"),
            @ApiResponse(responseCode = "504", description = "외부 서비스 응답 시간 초과")
    })
    @PostMapping
    public ResponseEntity<GlobalResponse<PortfolioAnalysisCreateResponseDto>> requestAnalysis(
            @Parameter(hidden = true)
            @RequestHeader(HEADER_USER_ID) UUID userId
    ) {
        PortfolioAnalysisCreateResponseDto response = portfolioAnalysisService.requestAnalysis(userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(GlobalResponse.success(HttpStatus.ACCEPTED.value(), response));
    }

    @Operation(
            summary = "최신 포트폴리오 AI 분석 조회",
            description = "가장 최근에 성공한 AI 분석 결과를 조회합니다."
    )
    @GetMapping("/latest")
    public ResponseEntity<GlobalResponse<PortfolioAnalysisResponseDto>> getLatestAnalysis(
            @Parameter(hidden = true)
            @RequestHeader(HEADER_USER_ID) UUID userId
    ) {
        PortfolioAnalysisResponseDto response = portfolioAnalysisService.getLatestAnalysis(userId);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }

    @Operation(
            summary = "포트폴리오 AI 분석 단건 조회",
            description = "분석 ID로 AI 분석 결과와 상태를 조회합니다."
    )
    @GetMapping("/{analysisId}")
    public ResponseEntity<GlobalResponse<PortfolioAnalysisResponseDto>> getAnalysis(
            @Parameter(hidden = true)
            @RequestHeader(HEADER_USER_ID) UUID userId,
            @PathVariable UUID analysisId
    ) {
        PortfolioAnalysisResponseDto response = portfolioAnalysisService.getAnalysis(userId, analysisId);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }

    @Operation(summary = "포트폴리오 AI 분석 이력 조회", description = "AI 분석 이력을 최신순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalResponse<PageRes<PortfolioAnalysisResponseDto>>> getAnalyses(
            @Parameter(hidden = true)
            @RequestHeader(HEADER_USER_ID) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRes<PortfolioAnalysisResponseDto> response = portfolioAnalysisService.getAnalyses(userId, page, size);
        return ResponseEntity.ok(GlobalResponse.success(HttpStatus.OK.value(), response));
    }
}
