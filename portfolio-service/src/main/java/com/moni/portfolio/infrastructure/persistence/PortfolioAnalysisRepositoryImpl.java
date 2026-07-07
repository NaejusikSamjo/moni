package com.moni.portfolio.infrastructure.persistence;

import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PortfolioAnalysisRepositoryImpl implements PortfolioAnalysisRepository {

    private final PortfolioAnalysisJpaRepository portfolioAnalysisJpaRepository;

    @Override
    public PortfolioAnalysis save(PortfolioAnalysis analysis) {
        return portfolioAnalysisJpaRepository.save(analysis);
    }

    @Override
    public Optional<PortfolioAnalysis> findById(UUID id) {
        return portfolioAnalysisJpaRepository.findById(id);
    }

    @Override
    public Optional<PortfolioAnalysis> findByIdAndPortfolioId(UUID id, UUID portfolioId) {
        return portfolioAnalysisJpaRepository.findByIdAndPortfolioId(id, portfolioId);
    }

    @Override
    public boolean existsByIdAndStatus(UUID id, AnalysisStatus status) {
        return portfolioAnalysisJpaRepository.existsByIdAndStatus(id, status);
    }

    @Override
    public Optional<PortfolioAnalysis> findLatestSuccessByPortfolioId(UUID portfolioId) {
        return portfolioAnalysisJpaRepository.findFirstByPortfolioIdAndStatusOrderByAnalyzedAtDesc(
                portfolioId,
                AnalysisStatus.SUCCESS
        );
    }

    @Override
    public Optional<PortfolioAnalysis> findPendingByPortfolioIdAndCreatedAtBetween(
            UUID portfolioId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        return portfolioAnalysisJpaRepository
                .findFirstByPortfolioIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        portfolioId,
                        AnalysisStatus.PENDING,
                        startDateTime,
                        endDateTime
                );
    }

    @Override
    public Page<PortfolioAnalysis> findAllByPortfolioIdAndStatusIn(
            UUID portfolioId,
            List<AnalysisStatus> statuses,
            Pageable pageable
    ) {
        return portfolioAnalysisJpaRepository.findAllByPortfolioIdAndStatusInOrderByAnalyzedAtDesc(
                portfolioId,
                statuses,
                pageable
        );
    }

    @Override
    public boolean existsByPortfolioIdAndStatusInAndCreatedAtBetween(
            UUID portfolioId,
            List<AnalysisStatus> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        return portfolioAnalysisJpaRepository
                .existsByPortfolioIdAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        portfolioId,
                        statuses,
                        startDateTime,
                        endDateTime
                );
    }

    @Override
    public int failPendingByPortfolioIdCreatedBefore(
            UUID portfolioId,
            LocalDateTime cutoffDateTime,
            String errorMessage,
            LocalDateTime analyzedAt
    ) {
        return portfolioAnalysisJpaRepository.failPendingByPortfolioIdCreatedBefore(
                AnalysisStatus.PENDING,
                AnalysisStatus.FAILED,
                portfolioId,
                cutoffDateTime,
                errorMessage,
                analyzedAt
        );
    }

    @Override
    public int failPendingByIdAndPortfolioIdCreatedBefore(
            UUID id,
            UUID portfolioId,
            LocalDateTime cutoffDateTime,
            String errorMessage,
            LocalDateTime analyzedAt
    ) {
        return portfolioAnalysisJpaRepository.failPendingByIdAndPortfolioIdCreatedBefore(
                AnalysisStatus.PENDING,
                AnalysisStatus.FAILED,
                id,
                portfolioId,
                cutoffDateTime,
                errorMessage,
                analyzedAt
        );
    }
}
