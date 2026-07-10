package com.moni.trade.holding.presentation.dto.response;

import com.moni.trade.holding.domain.entity.Holding;

import java.math.BigDecimal;
import java.util.UUID;

public record HoldingResponseDto(
        UUID id,
        String ticker,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal totalAmount
) {
    public static HoldingResponseDto from(Holding holding) {
        return new HoldingResponseDto(
                holding.getId(),
                holding.getTicker(),
                holding.getQuantity(),
                holding.getAveragePrice(),
                holding.getTotalAmount()
        );
    }
}
