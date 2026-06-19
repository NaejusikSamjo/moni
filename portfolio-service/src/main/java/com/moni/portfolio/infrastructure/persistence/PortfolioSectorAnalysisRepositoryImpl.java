package com.moni.portfolio.infrastructure.persistence;

import com.moni.portfolio.domain.entity.PortfolioSectorAnalysis;
import com.moni.portfolio.domain.repository.PortfolioSectorAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PortfolioSectorAnalysisRepositoryImpl implements PortfolioSectorAnalysisRepository {

    private final PortfolioSectorAnalysisJpaRepository portfolioSectorAnalysisJpaRepository;

    @Override
    public PortfolioSectorAnalysis save(PortfolioSectorAnalysis sectorAnalysis) {
        return portfolioSectorAnalysisJpaRepository.save(sectorAnalysis);
    }

    @Override
    public List<PortfolioSectorAnalysis> saveAll(List<PortfolioSectorAnalysis> sectorAnalyses) {
        return portfolioSectorAnalysisJpaRepository.saveAll(sectorAnalyses);
    }

    @Override
    public List<PortfolioSectorAnalysis> findAllByAnalysisId(UUID analysisId) {
        return portfolioSectorAnalysisJpaRepository.findAllByAnalysisId(analysisId);
    }
}
