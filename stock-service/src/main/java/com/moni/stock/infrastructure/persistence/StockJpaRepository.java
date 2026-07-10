package com.moni.stock.infrastructure.persistence;

import com.moni.stock.infrastructure.persistence.entity.StockEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockJpaRepository extends JpaRepository<StockEntity, UUID> {

    Optional<StockEntity> findByTicker(String ticker);

    List<StockEntity> findAllByTickerIn(List<String> tickers);

    @Modifying
    @Query(value = """
            INSERT INTO p_stock (id, ticker, name, market, created_at, updated_at)
            VALUES (gen_random_uuid(), :ticker, :name, :market, now(), now())
            ON CONFLICT (ticker) DO UPDATE SET
                name = EXCLUDED.name,
                market = EXCLUDED.market,
                updated_at = now()
            """, nativeQuery = true)
    void upsert(@Param("ticker") String ticker,
                @Param("name") String name,
                @Param("market") String market);

    Page<StockEntity> findByNameContaining(String name, Pageable pageable);

}