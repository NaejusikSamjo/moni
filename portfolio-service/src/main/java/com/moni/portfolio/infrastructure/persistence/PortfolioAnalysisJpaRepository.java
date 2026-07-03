package com.moni.portfolio.infrastructure.persistence;

import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

interface PortfolioAnalysisJpaRepository extends JpaRepository<PortfolioAnalysis, UUID> {

    Optional<PortfolioAnalysis> findByIdAndPortfolioId(UUID id, UUID portfolioId);

    Optional<PortfolioAnalysis> findFirstByPortfolioIdAndStatusOrderByAnalyzedAtDesc(UUID portfolioId, AnalysisStatus status);

    Page<PortfolioAnalysis> findAllByPortfolioIdOrderByAnalyzedAtDesc(UUID portfolioId, Pageable pageable);

    boolean existsByPortfolioIdAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
            UUID portfolioId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}
