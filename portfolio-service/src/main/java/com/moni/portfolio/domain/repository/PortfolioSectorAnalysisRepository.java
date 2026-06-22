package com.moni.portfolio.domain.repository;

import com.moni.portfolio.domain.entity.PortfolioSectorAnalysis;

import java.util.List;
import java.util.UUID;

public interface PortfolioSectorAnalysisRepository {

    PortfolioSectorAnalysis save(PortfolioSectorAnalysis sectorAnalysis);

    List<PortfolioSectorAnalysis> saveAll(List<PortfolioSectorAnalysis> sectorAnalyses);

    List<PortfolioSectorAnalysis> findAllByAnalysisId(UUID analysisId);
}
