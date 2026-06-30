package com.moni.ai.presentation.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PortfolioSectorAnalysisRequestDto(

        @NotBlank
        String sectorName,

        @NotNull
        @DecimalMin(value = "0.00")
        @DecimalMax(value = "100.00")
        BigDecimal weight,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal evaluationAmount
) {
}
