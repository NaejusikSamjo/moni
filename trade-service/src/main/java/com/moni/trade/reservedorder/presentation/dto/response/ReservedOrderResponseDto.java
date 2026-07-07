package com.moni.trade.reservedorder.presentation.dto.response;

import com.moni.trade.reservedorder.domain.entity.ReservedOrder;
import com.moni.trade.reservedorder.domain.enums.ReservedOrderStatus;
import com.moni.trade.reservedorder.domain.enums.ReservedOrderType;
import com.moni.trade.trade.domain.enums.TradeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservedOrderResponseDto(
        UUID id,
        String ticker,
        ReservedOrderType orderType,
        TradeType tradeType,
        BigDecimal targetPrice,
        BigDecimal amount,
        BigDecimal quantity,
        ReservedOrderStatus status,
        LocalDateTime createdAt
) {
    public static ReservedOrderResponseDto from(ReservedOrder order) {
        return new ReservedOrderResponseDto(
                order.getId(),
                order.getTicker(),
                order.getOrderType(),
                order.getTradeType(),
                order.getTargetPrice(),
                order.getAmount(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
