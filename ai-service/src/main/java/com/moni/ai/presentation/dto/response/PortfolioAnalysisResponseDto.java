package com.moni.ai.presentation.dto.response;

import java.util.UUID;

public record PortfolioAnalysisResponseDto(
        UUID analysisId,
        String summary,
        PortfolioTendencyAnalysisResponseDto tendencyAnalysis,
        String recommendation
) {
}
