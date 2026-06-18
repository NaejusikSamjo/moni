package com.moni.portfolio.application.calculator.model;

import java.math.BigDecimal;

public record HoldingInput(
        String ticker,
        Long quantity,
        BigDecimal averagePurchasePrice
) {
}
