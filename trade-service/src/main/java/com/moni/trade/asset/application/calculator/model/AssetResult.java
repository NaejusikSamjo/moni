package com.moni.trade.asset.application.calculator.model;

import java.math.BigDecimal;
import java.util.List;

public record AssetResult(
        BigDecimal totalAsset,
        BigDecimal cashBalance,
        BigDecimal stockEvaluationAmount,
        BigDecimal principalAmount,
        BigDecimal totalProfitLoss,
        BigDecimal totalReturnRate,
        List<HoldingResult> holdings
) {
}
