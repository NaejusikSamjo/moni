package com.moni.stock.infrastructure.persistence.entity;

import com.moni.common.JpaAuditing.baseEntity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Table(name = "p_stock_theme")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockThemeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private StockEntity stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private ThemeEntity theme;

    @Builder
    private StockThemeEntity(StockEntity stock, ThemeEntity theme) {
        this.stock = stock;
        this.theme = theme;
    }
}