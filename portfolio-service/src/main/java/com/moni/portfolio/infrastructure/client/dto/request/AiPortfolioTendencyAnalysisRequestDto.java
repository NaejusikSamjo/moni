package com.moni.portfolio.infrastructure.client.dto.request;

public record AiPortfolioTendencyAnalysisRequestDto(
        String userTendencyType,
        String userTendencyLabel,
        Integer userTendencyScore,
        String portfolioRiskType,
        String portfolioRiskLabel,
        Integer portfolioRiskScore,
        Integer suitabilityScore,
        String suitabilityLevel
) {
}
