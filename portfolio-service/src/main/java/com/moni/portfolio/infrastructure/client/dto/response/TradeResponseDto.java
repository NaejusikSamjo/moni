package com.moni.portfolio.infrastructure.client.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TradeResponseDto(
        UUID id,
        String ticker,
        String tradeType,
        Integer quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        BigDecimal profitAmount,
        BigDecimal profitRate,
        String status,
        LocalDateTime createdAt
) {
}
