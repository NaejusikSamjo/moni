package com.moni.portfolio.application.calculator.model;

import java.math.BigDecimal;

public record TradeInput(
        String tradeType,
        BigDecimal totalAmount,
        String status
) {
}
