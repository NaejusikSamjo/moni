package com.moni.trade.reservedorder.presentation.dto.request;

import com.moni.trade.reservedorder.domain.enums.ReservedOrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ReservedBuyOrderRequestDto(
        @NotBlank String ticker,
        @NotNull ReservedOrderType orderType,
        @DecimalMin("0.01") BigDecimal targetPrice,
        @NotNull @Positive BigDecimal amount
) {
}
