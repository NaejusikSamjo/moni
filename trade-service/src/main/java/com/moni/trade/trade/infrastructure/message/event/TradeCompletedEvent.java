package com.moni.trade.trade.infrastructure.message.event;

import com.moni.trade.trade.domain.enums.TradeType;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeCompletedEvent(
        UUID tradeId,
        UUID accountId,
        String ticker,
        TradeType tradeType,
        Integer quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        BigDecimal profitAmount,
        BigDecimal profitRate
) {
}
