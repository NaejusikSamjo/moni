package com.moni.ai.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PortfolioAnalysisRequestDto(

        @NotNull
        UUID analysisId,

        @NotNull
        UUID userId,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal totalEvaluationAmount,

        @NotNull
        BigDecimal totalReturnRate,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal concentrationScore,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal concentrationThreshold,

        @NotEmpty
        List<PortfolioSectorAnalysisRequestDto> sectorAnalyses,

        @NotEmpty
        List<PortfolioHoldingRequestDto> holdings,

        @Valid
        PortfolioTendencyAnalysisRequestDto tendencyAnalysis
) {
}
