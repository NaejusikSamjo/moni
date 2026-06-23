package com.moni.trade.trade.infrastructure.client.dto;

import java.math.BigDecimal;

public record StockPriceResponseDto(
        String ticker,
        String name,
        BigDecimal price
) {
}
