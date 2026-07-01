package com.moni.ai.presentation.dto.response;

import com.moni.ai.infrastructure.client.LlmTendencyAnalysisResponse;
import com.moni.ai.presentation.dto.request.PortfolioTendencyAnalysisRequestDto;

public record PortfolioTendencyAnalysisResponseDto(
        String userTendencyType,
        String userTendencyLabel,
        Integer userTendencyScore,
        String portfolioRiskType,
        String portfolioRiskLabel,
        Integer portfolioRiskScore,
        Integer suitabilityScore,
        String suitabilityLevel,
        String summary,
        String recommendation
) {
    public static PortfolioTendencyAnalysisResponseDto from(
            PortfolioTendencyAnalysisRequestDto request,
            LlmTendencyAnalysisResponse response
    ) {
        if (request == null || response == null) {
            return null;
        }

        return new PortfolioTendencyAnalysisResponseDto(
                request.userTendencyType(),
                request.userTendencyLabel(),
                request.userTendencyScore(),
                request.portfolioRiskType(),
                request.portfolioRiskLabel(),
                request.portfolioRiskScore(),
                request.suitabilityScore(),
                request.suitabilityLevel(),
                response.summary(),
                response.recommendation()
        );
    }
}
