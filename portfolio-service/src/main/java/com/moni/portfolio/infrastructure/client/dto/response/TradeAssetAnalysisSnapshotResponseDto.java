package com.moni.portfolio.infrastructure.client.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record TradeAssetAnalysisSnapshotResponseDto(
        BigDecimal totalAsset,
        BigDecimal cashBalance,
        BigDecimal stockEvaluationAmount,
        BigDecimal principalAmount,
        BigDecimal totalProfitLoss,
        BigDecimal totalReturnRate,
        BigDecimal stockProfitLoss,
        BigDecimal stockReturnRate,
        List<TradeAssetHoldingResponseDto> holdings
) {
}
