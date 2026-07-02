package com.moni.portfolio.presentation.dto.response;

import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;

import java.util.UUID;

public record PortfolioAnalysisCreateResponseDto(
        UUID analysisId,
        AnalysisStatus status
) {
    public static PortfolioAnalysisCreateResponseDto from(PortfolioAnalysis analysis) {
        return new PortfolioAnalysisCreateResponseDto(analysis.getId(), analysis.getStatus());
    }
}
