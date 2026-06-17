package com.moni.portfolio.infrastructure.persistence;

import com.moni.portfolio.domain.entity.PortfolioSectorAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface PortfolioSectorAnalysisJpaRepository extends JpaRepository<PortfolioSectorAnalysis, UUID> {

    List<PortfolioSectorAnalysis> findAllByAnalysisId(UUID analysisId);
}
