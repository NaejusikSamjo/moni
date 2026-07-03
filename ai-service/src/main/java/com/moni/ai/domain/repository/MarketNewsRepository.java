package com.moni.ai.domain.repository;

import com.moni.ai.domain.entity.MarketNewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MarketNewsRepository extends JpaRepository<MarketNewsEntity, UUID> {
    boolean existsByUrl(String url);
}
