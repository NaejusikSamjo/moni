package com.moni.portfolio.infrastructure.client.dto.request;

import java.math.BigDecimal;

public record AiPortfolioHoldingRequestDto(
        String ticker,
        String stockName,
        String sectorName,
        Long quantity,
        BigDecimal averagePurchasePrice,
        BigDecimal currentPrice,
        BigDecimal evaluationAmount,
        BigDecimal profitLoss,
        BigDecimal profitRate,
        BigDecimal weight
) {
}
