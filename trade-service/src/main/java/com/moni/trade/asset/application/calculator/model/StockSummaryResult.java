package com.moni.trade.asset.application.calculator.model;

import java.math.BigDecimal;

public record StockSummaryResult(
        BigDecimal stockProfitLoss,
        BigDecimal stockReturnRate
) {
}
