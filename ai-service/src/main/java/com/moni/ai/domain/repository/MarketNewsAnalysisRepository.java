package com.moni.ai.domain.repository;

import com.moni.ai.domain.entity.MarketNewsAnalysisEntity;
import com.moni.ai.domain.entity.MarketNewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface MarketNewsAnalysisRepository extends JpaRepository<MarketNewsAnalysisEntity, UUID>{
    Optional<MarketNewsAnalysisEntity> findTopByKeywordAndExpiredAtAfterOrderByCreatedAtDesc(String keyword, LocalDateTime expiredAt);
}
