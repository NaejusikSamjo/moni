package com.moni.portfolio.application.analysis;

import com.moni.portfolio.infrastructure.client.dto.request.AiPortfolioTendencyAnalysisRequestDto;

public record TendencySuitabilityResult(
        TendencyType userTendencyType,
        int userTendencyScore,
        TendencyType portfolioRiskType,
        int portfolioRiskScore,
        int suitabilityScore,
        String suitabilityLevel
) {
    public AiPortfolioTendencyAnalysisRequestDto toAiRequest() {
        return new AiPortfolioTendencyAnalysisRequestDto(
                userTendencyType.name(),
                userTendencyType.label(),
                userTendencyScore,
                portfolioRiskType.name(),
                portfolioRiskType.label(),
                portfolioRiskScore,
                suitabilityScore,
                suitabilityLevel
        );
    }
}
