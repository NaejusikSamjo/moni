package com.moni.portfolio.infrastructure.client;

import com.moni.portfolio.infrastructure.client.config.FeignConfig;
import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioAnalysisRequestDto;
import com.moni.portfolio.infrastructure.client.dto.response.AiPortfolioAnalysisResponseDto;
import com.moni.portfolio.infrastructure.client.dto.response.ExternalApiResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "ai-service", configuration = FeignConfig.class)
public interface AiServiceClient {

    @PostMapping("/api/v1/ai/portfolio/analysis")
    ExternalApiResponseDto<AiPortfolioAnalysisResponseDto> analyzePortfolio(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody AiPortfolioAnalysisRequestDto request
    );
}
