package com.moni.trade.trade.presentation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TradeBuyRequestDto(
        @NotBlank String ticker,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
