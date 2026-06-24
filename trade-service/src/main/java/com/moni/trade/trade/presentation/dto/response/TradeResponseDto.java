package com.moni.trade.trade.presentation.dto.response;

import com.moni.trade.trade.domain.entity.Trade;
import com.moni.trade.trade.domain.enums.TradeStatus;
import com.moni.trade.trade.domain.enums.TradeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TradeResponseDto(
        UUID id,
        String ticker,
        TradeType tradeType,
        Integer quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        BigDecimal profitAmount,
        BigDecimal profitRate,
        TradeStatus status,
        LocalDateTime createdAt
) {
    public static TradeResponseDto from(Trade trade) {
        return new TradeResponseDto(
                trade.getId(),
                trade.getTicker(),
                trade.getTradeType(),
                trade.getQuantity(),
                trade.getPrice(),
                trade.getTotalAmount(),
                trade.getProfitAmount(),
                trade.getProfitRate(),
                trade.getStatus(),
                trade.getCreatedAt()
        );
    }
}
