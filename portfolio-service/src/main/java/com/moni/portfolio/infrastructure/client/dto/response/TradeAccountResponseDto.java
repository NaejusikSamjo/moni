package com.moni.portfolio.infrastructure.client.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TradeAccountResponseDto(
        UUID id,
        UUID userId,
        BigDecimal balance,
        BigDecimal totalInvestment
) {
}
