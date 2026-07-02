package com.moni.ai.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PortfolioTendencyAnalysisRequestDto(

        @NotBlank
        String userTendencyType,

        @NotBlank
        String userTendencyLabel,

        @NotNull
        @Min(0)
        @Max(100)
        Integer userTendencyScore,

        @NotBlank
        String portfolioRiskType,

        @NotBlank
        String portfolioRiskLabel,

        @NotNull
        @Min(0)
        @Max(100)
        Integer portfolioRiskScore,

        @NotNull
        @Min(0)
        @Max(100)
        Integer suitabilityScore,

        @NotBlank
        String suitabilityLevel
) {
}
