package com.moni.trade.reservedorder.presentation.dto.request;

import com.moni.trade.reservedorder.domain.enums.ReservedOrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReservedSellOrderRequestDto(
        @NotBlank String ticker,
        @NotNull ReservedOrderType orderType,
        @DecimalMin("0.01") BigDecimal targetPrice,
        @NotNull @DecimalMin("0.01") BigDecimal quantity
) {
}
