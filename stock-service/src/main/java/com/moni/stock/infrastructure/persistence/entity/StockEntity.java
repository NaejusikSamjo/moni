package com.moni.stock.infrastructure.persistence.entity;

import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import com.moni.stock.domain.entity.Stock;
import com.moni.stock.domain.type.MarketType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_stock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 10)
    private String ticker;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MarketType market;

    @Builder
    private StockEntity(String ticker, String name, MarketType market) {
        this.ticker = ticker;
        this.name = name;
        this.market = market;
    }

    public void update(Stock stock) {
        this.name = stock.getName();
        this.market = stock.getMarket();
    }

    public static StockEntity from(Stock stock) {
        return StockEntity.builder()
                .ticker(stock.getTicker())
                .name(stock.getName())
                .market(stock.getMarket())
                .build();
    }

    public Stock toDomain() {
        return Stock.builder()
                .id(id)
                .ticker(ticker)
                .name(name)
                .market(market)
                .build();
    }
}