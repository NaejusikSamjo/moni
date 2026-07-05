package com.moni.portfolio.infrastructure.persistence;

import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import com.moni.portfolio.domain.repository.PortfolioAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    public Optional<PortfolioAnalysis> findLatestSuccessByPortfolioId(UUID portfolioId) {
        return portfolioAnalysisJpaRepository.findFirstByPortfolioIdAndStatusOrderByAnalyzedAtDesc(
                portfolioId,
                AnalysisStatus.SUCCESS
        );
    }

    @Override
    public Page<PortfolioAnalysis> findAllByPortfolioId(UUID portfolioId, Pageable pageable) {
        return portfolioAnalysisJpaRepository.findAllByPortfolioIdOrderByAnalyzedAtDesc(portfolioId, pageable);
    }

    @Override
    public boolean existsByPortfolioIdAndCreatedAtBetween(
            UUID portfolioId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    ) {
        return portfolioAnalysisJpaRepository.existsByPortfolioIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                portfolioId,
                startDateTime,
                endDateTime
        );
    }
}
