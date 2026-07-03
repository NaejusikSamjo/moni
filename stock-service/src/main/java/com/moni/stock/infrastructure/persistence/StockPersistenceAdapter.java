package com.moni.stock.infrastructure.persistence;

import com.moni.stock.domain.entity.Stock;
import com.moni.stock.domain.repository.StockRepository;
import com.moni.stock.infrastructure.persistence.entity.StockEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockPersistenceAdapter implements StockRepository {

    private final StockJpaRepository stockJpaRepository;

    @Override
    public Page<Stock> findAll(Pageable pageable) {
        return stockJpaRepository.findAll(pageable).map(StockEntity::toDomain);
    }

    @Override
    public Page<Stock> findByNameContaining(String keyword, Pageable pageable) {
        return stockJpaRepository.findByNameContaining(keyword, pageable).map(StockEntity::toDomain);
    }

    @Override
    public Optional<Stock> findByTicker(String ticker) {
        return stockJpaRepository.findByTicker(ticker).map(StockEntity::toDomain);
    }

    @Override
    @Transactional
    public void saveAll(List<Stock> stocks) {
        stocks.forEach(stock ->
                stockJpaRepository.upsert(stock.getTicker(), stock.getName(), stock.getMarket().name())
        );
    }

    @Override
    public void save(Stock stock) {
        stockJpaRepository.save(StockEntity.from(stock));
    }

    @Override
    public List<Stock> findByTickerIn(List<String> tickers) {
        return stockJpaRepository.findAllByTickerIn(tickers)
                .stream()
                .map(StockEntity::toDomain)
                .toList();
    }
}