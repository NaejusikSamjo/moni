package com.moni.portfolio.infrastructure.persistence;

import com.moni.portfolio.domain.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PortfolioJpaRepository extends JpaRepository<Portfolio, UUID> {

    Optional<Portfolio> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
