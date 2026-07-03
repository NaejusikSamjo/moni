package com.moni.portfolio.infrastructure.client.dto.response;

import java.math.BigDecimal;

public record TradeAssetResponseDto(
        BigDecimal totalAsset,
        BigDecimal cashBalance,
        BigDecimal stockEvaluationAmount,
        BigDecimal principalAmount,
        BigDecimal totalProfitLoss,
        BigDecimal totalReturnRate
) {
}
