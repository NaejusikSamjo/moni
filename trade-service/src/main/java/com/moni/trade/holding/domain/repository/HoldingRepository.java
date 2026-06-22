package com.moni.trade.holding.domain.repository;

import com.moni.trade.holding.domain.entity.Holding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HoldingRepository extends JpaRepository<Holding, UUID> {

    Page<Holding> findByAccountId(UUID accountId, Pageable pageable);

    Optional<Holding> findByAccountIdAndTicker(UUID accountId, String ticker);

    boolean existsByAccountIdAndTicker(UUID accountId, String ticker);
}
