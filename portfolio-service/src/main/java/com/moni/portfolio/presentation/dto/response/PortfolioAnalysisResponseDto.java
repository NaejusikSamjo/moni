package com.moni.portfolio.presentation.dto.response;

import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PortfolioAnalysisResponseDto(
        UUID analysisId,
        AnalysisStatus status,
        BigDecimal totalReturnRate,
        BigDecimal totalEvaluationAmount,
        String summary,
        BigDecimal concentrationScore,
        BigDecimal concentrationThreshold,
        String errorMessage,
        LocalDateTime analyzedAt
) {
    public static PortfolioAnalysisResponseDto from(PortfolioAnalysis analysis) {
        return new PortfolioAnalysisResponseDto(
                analysis.getId(),
                analysis.getStatus(),
                analysis.getTotalReturnRate(),
                analysis.getTotalEvaluationAmount(),
                analysis.getSummary(),
                analysis.getConcentrationScore(),
                analysis.getConcentrationThreshold(),
                analysis.getErrorMessage(),
                analysis.getAnalyzedAt()
        );
    }
}
