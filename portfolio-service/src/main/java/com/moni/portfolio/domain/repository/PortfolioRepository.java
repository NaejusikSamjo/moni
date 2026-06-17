package com.moni.portfolio.domain.repository;

import com.moni.portfolio.domain.entity.Portfolio;

import java.util.Optional;
import java.util.UUID;

public interface PortfolioRepository {

    Portfolio save(Portfolio portfolio);

    Optional<Portfolio> findById(UUID id);

    Optional<Portfolio> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
