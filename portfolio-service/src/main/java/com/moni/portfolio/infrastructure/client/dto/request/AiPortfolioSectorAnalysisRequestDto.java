package com.moni.portfolio.infrastructure.client.dto.request;

import java.math.BigDecimal;

public record AiPortfolioSectorAnalysisRequestDto(
        String sectorName,
        BigDecimal weight,
        BigDecimal evaluationAmount
) {
}
