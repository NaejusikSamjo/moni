package com.moni.trade.asset.application.calculator.model;

import java.math.BigDecimal;

public record HoldingInput(
        String ticker,
        Long quantity,
        BigDecimal averagePurchasePrice,
        BigDecimal totalPurchaseAmount
) {
}
