package com.moni.user.user.domain.repository;

import com.moni.user.user.domain.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {

    List<Watchlist> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<Watchlist> findByUserIdAndStockCodeAndDeletedAtIsNull(UUID userId, String stockCode);

    boolean existsByUserIdAndStockCodeAndDeletedAtIsNull(UUID userId, String stockCode);
}