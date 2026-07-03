package com.moni.ai.domain.repository;

import com.moni.ai.domain.entity.CompanyIssueAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface CompanyIssueAnalysisRepository extends JpaRepository<CompanyIssueAnalysisEntity, UUID>, CompanyIssueAnalysisRepositoryCustom {

    Optional<CompanyIssueAnalysisEntity> findTopByTickerAndExpiredAtAfterOrderByCreatedAtDesc(String ticker, LocalDateTime now);
}
