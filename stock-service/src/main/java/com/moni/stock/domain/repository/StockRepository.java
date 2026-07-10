package com.moni.stock.domain.repository;

import com.moni.stock.domain.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StockRepository {

    Page<Stock> findAll(Pageable pageable);

    Page<Stock> findByNameContaining(String keyword,Pageable pageable);

    Optional<Stock> findByTicker(String ticker);

    void saveAll(List<Stock> stocks);

    void save(Stock stock);

    List<Stock> findByTickerIn(List<String> tickers);
}