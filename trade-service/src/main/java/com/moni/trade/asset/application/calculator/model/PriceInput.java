package com.moni.trade.asset.application.calculator.model;

import java.math.BigDecimal;

public record PriceInput(
        String ticker,
        BigDecimal currentPrice
) {
}
