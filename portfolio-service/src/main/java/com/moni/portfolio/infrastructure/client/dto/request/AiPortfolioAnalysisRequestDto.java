package com.moni.portfolio.infrastructure.client.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AiPortfolioAnalysisRequestDto(
        UUID analysisId,
        UUID userId,
        BigDecimal totalEvaluationAmount,
        BigDecimal totalReturnRate,
        BigDecimal concentrationScore,
        BigDecimal concentrationThreshold,
        List<AiPortfolioHoldingRequestDto> holdings,
        AiPortfolioTendencyAnalysisRequestDto tendencyAnalysis
) {
}
