package com.moni.portfolio.presentation.controller;

import com.moni.common.response.GlobalResponse;
import com.moni.portfolio.application.service.PortfolioService;
import com.moni.portfolio.presentation.dto.response.PortfolioCreateResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Portfolio", description = "포트폴리오 서비스 API")
@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final PortfolioService portfolioService;

    @Operation(summary = "포트폴리오 생성", description = "현재 사용자의 포트폴리오를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "포트폴리오 생성 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청"),
            @ApiResponse(responseCode = "409", description = "PORTFOLIO-001: 이미 포트폴리오가 존재함")
    })
    @PostMapping
    public ResponseEntity<GlobalResponse<PortfolioCreateResponseDto>> createPortfolio(
            @Parameter(hidden = true)
            @RequestHeader(HEADER_USER_ID) UUID userId
    ) {
        PortfolioCreateResponseDto response = portfolioService.createPortfolio(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(HttpStatus.CREATED.value(), response));
    }
}
