package com.moni.trade.asset.application.calculator.model;

import java.math.BigDecimal;

public record AssetResult(
        BigDecimal totalAsset,
        BigDecimal cashBalance,
        BigDecimal stockEvaluationAmount,
        BigDecimal principalAmount,
        BigDecimal totalProfitLoss,
        BigDecimal totalReturnRate
) {
}
