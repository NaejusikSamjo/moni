package com.moni.ai.presentation.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PortfolioHoldingRequestDto(

        @NotBlank
        String ticker,

        @NotBlank
        String stockName,

        @NotBlank
        String sectorName,

        @NotNull
        @Positive
        Long quantity,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal averagePurchasePrice,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal currentPrice,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal evaluationAmount,

        @NotNull
        BigDecimal profitLoss,

        @NotNull
        BigDecimal profitRate,

        @NotNull
        @DecimalMin(value = "0.00")
        @DecimalMax(value = "100.00")
        BigDecimal weight
) {
}
