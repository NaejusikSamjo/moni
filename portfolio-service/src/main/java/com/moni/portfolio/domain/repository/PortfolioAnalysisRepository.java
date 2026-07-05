package com.moni.portfolio.domain.repository;

import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioAnalysisRepository {

    PortfolioAnalysis save(PortfolioAnalysis analysis);

    Optional<PortfolioAnalysis> findById(UUID id);

    Optional<PortfolioAnalysis> findByIdAndPortfolioId(UUID id, UUID portfolioId);

    Optional<PortfolioAnalysis> findLatestSuccessByPortfolioId(UUID portfolioId);

    Page<PortfolioAnalysis> findAllByPortfolioId(UUID portfolioId, Pageable pageable);

    boolean existsByPortfolioIdAndCreatedAtBetween(
            UUID portfolioId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}
