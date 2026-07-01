package com.moni.portfolio.presentation.dto.response;

import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.entity.PortfolioSectorAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
        LocalDateTime analyzedAt,
        List<PortfolioSectorAnalysisResponseDto> sectorAnalyses
) {
    public static PortfolioAnalysisResponseDto from(
            PortfolioAnalysis analysis,
            List<PortfolioSectorAnalysis> sectorAnalyses
    ) {
        return new PortfolioAnalysisResponseDto(
                analysis.getId(),
                analysis.getStatus(),
                analysis.getTotalReturnRate(),
                analysis.getTotalEvaluationAmount(),
                analysis.getSummary(),
                analysis.getConcentrationScore(),
                analysis.getConcentrationThreshold(),
                analysis.getErrorMessage(),
                analysis.getAnalyzedAt(),
                sectorAnalyses.stream()
                        .map(PortfolioSectorAnalysisResponseDto::from)
                        .toList()
        );
    }
}
