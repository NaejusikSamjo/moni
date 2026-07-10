package com.moni.portfolio.domain.repository;

import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioAnalysisRepository {

    PortfolioAnalysis save(PortfolioAnalysis analysis);

    Optional<PortfolioAnalysis> findById(UUID id);

    Optional<PortfolioAnalysis> findByIdAndPortfolioId(UUID id, UUID portfolioId);

    boolean existsByIdAndStatus(UUID id, AnalysisStatus status);

    Optional<PortfolioAnalysis> findLatestSuccessByPortfolioId(UUID portfolioId);

    Optional<PortfolioAnalysis> findPendingByPortfolioIdAndCreatedAtBetween(
            UUID portfolioId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    Page<PortfolioAnalysis> findAllByPortfolioIdAndStatusIn(
            UUID portfolioId,
            List<AnalysisStatus> statuses,
            Pageable pageable
    );

    boolean existsByPortfolioIdAndStatusInAndCreatedAtBetween(
            UUID portfolioId,
            List<AnalysisStatus> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    int failPendingByPortfolioIdCreatedBefore(
            UUID portfolioId,
            LocalDateTime cutoffDateTime,
            String errorMessage,
            LocalDateTime analyzedAt
    );

    int failPendingByIdAndPortfolioIdCreatedBefore(
            UUID id,
            UUID portfolioId,
            LocalDateTime cutoffDateTime,
            String errorMessage,
            LocalDateTime analyzedAt
    );
}
