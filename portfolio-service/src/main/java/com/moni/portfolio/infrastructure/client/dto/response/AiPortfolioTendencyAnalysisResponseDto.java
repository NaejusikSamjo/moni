package com.moni.portfolio.infrastructure.client.dto.response;

public record AiPortfolioTendencyAnalysisResponseDto(
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
}
