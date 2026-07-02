package com.moni.portfolio.infrastructure.persistence;

import com.moni.portfolio.domain.entity.Portfolio;
import com.moni.portfolio.domain.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PortfolioRepositoryImpl implements PortfolioRepository {

    private final PortfolioJpaRepository portfolioJpaRepository;

    @Override
    public Portfolio save(Portfolio portfolio) {
        return portfolioJpaRepository.save(portfolio);
    }

    @Override
    public Optional<Portfolio> findById(UUID id) {
        return portfolioJpaRepository.findById(id);
    }

    @Override
    public Optional<Portfolio> findByUserId(UUID userId) {
        return portfolioJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<Portfolio> findByUserIdForUpdate(UUID userId) {
        return portfolioJpaRepository.findByUserIdForUpdate(userId);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return portfolioJpaRepository.existsByUserId(userId);
    }
}
