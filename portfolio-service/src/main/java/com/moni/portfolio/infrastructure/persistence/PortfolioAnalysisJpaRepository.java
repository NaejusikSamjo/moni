package com.moni.portfolio.infrastructure.persistence;

import com.moni.portfolio.domain.entity.PortfolioAnalysis;
import com.moni.portfolio.domain.enums.AnalysisStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PortfolioAnalysisJpaRepository extends JpaRepository<PortfolioAnalysis, UUID> {

    Optional<PortfolioAnalysis> findByIdAndPortfolioId(UUID id, UUID portfolioId);

    boolean existsByIdAndStatus(UUID id, AnalysisStatus status);

    Optional<PortfolioAnalysis> findFirstByPortfolioIdAndStatusOrderByAnalyzedAtDesc(UUID portfolioId, AnalysisStatus status);

    Optional<PortfolioAnalysis> findFirstByPortfolioIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            UUID portfolioId,
            AnalysisStatus status,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    Page<PortfolioAnalysis> findAllByPortfolioIdAndStatusInOrderByAnalyzedAtDesc(
            UUID portfolioId,
            List<AnalysisStatus> statuses,
            Pageable pageable
    );

    boolean existsByPortfolioIdAndStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID portfolioId,
            List<AnalysisStatus> statuses,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            update PortfolioAnalysis analysis
            set analysis.status = :failedStatus,
                analysis.errorMessage = :errorMessage,
                analysis.analyzedAt = :analyzedAt,
                analysis.updatedAt = :analyzedAt
            where analysis.status = :pendingStatus
              and analysis.portfolio.id = :portfolioId
              and analysis.createdAt <= :cutoffDateTime
            """)
    int failPendingByPortfolioIdCreatedBefore(
            @Param("pendingStatus") AnalysisStatus pendingStatus,
            @Param("failedStatus") AnalysisStatus failedStatus,
            @Param("portfolioId") UUID portfolioId,
            @Param("cutoffDateTime") LocalDateTime cutoffDateTime,
            @Param("errorMessage") String errorMessage,
            @Param("analyzedAt") LocalDateTime analyzedAt
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            update PortfolioAnalysis analysis
            set analysis.status = :failedStatus,
                analysis.errorMessage = :errorMessage,
                analysis.analyzedAt = :analyzedAt,
                analysis.updatedAt = :analyzedAt
            where analysis.id = :id
              and analysis.status = :pendingStatus
              and analysis.portfolio.id = :portfolioId
              and analysis.createdAt <= :cutoffDateTime
            """)
    int failPendingByIdAndPortfolioIdCreatedBefore(
            @Param("pendingStatus") AnalysisStatus pendingStatus,
            @Param("failedStatus") AnalysisStatus failedStatus,
            @Param("id") UUID id,
            @Param("portfolioId") UUID portfolioId,
            @Param("cutoffDateTime") LocalDateTime cutoffDateTime,
            @Param("errorMessage") String errorMessage,
            @Param("analyzedAt") LocalDateTime analyzedAt
    );
}
