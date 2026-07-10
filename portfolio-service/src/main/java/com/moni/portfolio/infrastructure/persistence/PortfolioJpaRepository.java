package com.moni.portfolio.infrastructure.persistence;

import com.moni.portfolio.domain.entity.Portfolio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface PortfolioJpaRepository extends JpaRepository<Portfolio, UUID> {

    Optional<Portfolio> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Portfolio p where p.userId = :userId")
    Optional<Portfolio> findByUserIdForUpdate(@Param("userId") UUID userId);

    boolean existsByUserId(UUID userId);
}
