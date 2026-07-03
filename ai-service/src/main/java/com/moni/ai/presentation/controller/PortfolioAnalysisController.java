package com.moni.ai.presentation.controller;

import com.moni.ai.application.service.PortfolioAnalysisService;
import com.moni.ai.presentation.dto.request.PortfolioAnalysisRequestDto;
import com.moni.ai.presentation.dto.response.PortfolioAnalysisResponseDto;
import com.moni.common.response.GlobalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/portfolio")
@RequiredArgsConstructor
public class PortfolioAnalysisController {

    private final PortfolioAnalysisService portfolioAnalysisService;

    @PostMapping("/analysis")
    public ResponseEntity<GlobalResponse<PortfolioAnalysisResponseDto>> analyze(
            @Valid @RequestBody PortfolioAnalysisRequestDto request
    ) {
        PortfolioAnalysisResponseDto response = portfolioAnalysisService.analyze(request);
        return ResponseEntity.ok(GlobalResponse.success(200, response));
    }
}
