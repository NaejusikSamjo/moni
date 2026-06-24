package com.moni.trade.trade.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TradeSellRequestDto(
        @NotBlank String ticker,
        @Positive Integer quantity
) {
}
