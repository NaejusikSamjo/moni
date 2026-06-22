package com.moni.portfolio.infrastructure.client.dto.response;

import java.math.BigDecimal;

public record StockResponseDto(
        String ticker,
        String name,
        BigDecimal price
) {
}
