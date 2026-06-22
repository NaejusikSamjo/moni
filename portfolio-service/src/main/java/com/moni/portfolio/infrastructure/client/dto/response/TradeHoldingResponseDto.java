package com.moni.portfolio.infrastructure.client.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeHoldingResponseDto(
        UUID id,
        String ticker,
        Integer quantity,
        BigDecimal averagePrice,
        BigDecimal totalAmount
) {
}
