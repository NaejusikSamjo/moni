package com.moni.portfolio.infrastructure.client.dto.response;

import java.util.UUID;

public record AiPortfolioAnalysisResponseDto(
        UUID analysisId,
        String summary,
        AiPortfolioTendencyAnalysisResponseDto tendencyAnalysis,
        String recommendation
) {
}
