package com.moni.portfolio.infrastructure.client.dto.response;

import java.math.BigDecimal;

public record TradeAssetHoldingResponseDto(
        String ticker,
        Long quantity,
        BigDecimal averagePurchasePrice,
        BigDecimal currentPrice,
        BigDecimal evaluationAmount,
        BigDecimal profitLoss,
        BigDecimal profitRate,
        BigDecimal weight
) {
}
