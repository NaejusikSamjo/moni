package com.moni.trade.asset.application.calculator.model;

import com.moni.trade.trade.domain.enums.TradeStatus;
import com.moni.trade.trade.domain.enums.TradeType;

import java.math.BigDecimal;

public record TradeInput(
        TradeType tradeType,
        BigDecimal totalAmount,
        TradeStatus status
) {
}
