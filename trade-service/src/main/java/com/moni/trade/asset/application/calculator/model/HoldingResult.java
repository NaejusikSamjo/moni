package com.moni.trade.asset.application.calculator.model;

import java.math.BigDecimal;

public record HoldingResult(
        String ticker,
        BigDecimal quantity,
        BigDecimal averagePurchasePrice,
        BigDecimal currentPrice,
        BigDecimal evaluationAmount,
        BigDecimal profitLoss,
        BigDecimal profitRate,
        BigDecimal weight
) {
    public HoldingResult withWeight(BigDecimal weight) {
        return new HoldingResult(
                ticker,
                quantity,
                averagePurchasePrice,
                currentPrice,
                evaluationAmount,
                profitLoss,
                profitRate,
                weight
        );
    }
}
