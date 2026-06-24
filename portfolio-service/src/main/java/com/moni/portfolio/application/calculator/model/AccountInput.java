package com.moni.portfolio.application.calculator.model;

import java.math.BigDecimal;

public record AccountInput(
        BigDecimal cashBalance,
        BigDecimal principalAmount
) {
}
