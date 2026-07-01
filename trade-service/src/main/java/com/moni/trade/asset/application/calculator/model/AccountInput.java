package com.moni.trade.asset.application.calculator.model;

import java.math.BigDecimal;

public record AccountInput(
        BigDecimal cashBalance,
        BigDecimal principalAmount
) {
}
