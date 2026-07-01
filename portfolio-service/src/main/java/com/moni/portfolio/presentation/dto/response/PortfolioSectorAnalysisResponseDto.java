package com.moni.portfolio.presentation.dto.response;

import com.moni.portfolio.domain.entity.PortfolioSectorAnalysis;

import java.math.BigDecimal;

public record PortfolioSectorAnalysisResponseDto(
        String sectorName,
        BigDecimal weight,
        BigDecimal evaluationAmount
) {
    public static PortfolioSectorAnalysisResponseDto from(PortfolioSectorAnalysis sectorAnalysis) {
        return new PortfolioSectorAnalysisResponseDto(
                sectorAnalysis.getSectorName(),
                sectorAnalysis.getWeight(),
                sectorAnalysis.getEvaluationAmount()
        );
    }
}
