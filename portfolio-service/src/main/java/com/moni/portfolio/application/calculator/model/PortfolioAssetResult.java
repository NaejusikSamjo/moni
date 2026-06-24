package com.moni.portfolio.application.calculator.model;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioAssetResult(
        BigDecimal totalAsset,
        BigDecimal cashBalance,
        BigDecimal stockEvaluationAmount,
        BigDecimal principalAmount,
        BigDecimal totalProfitLoss,
        BigDecimal totalReturnRate,
        List<HoldingResult> holdings
) {
}
