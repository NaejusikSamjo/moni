package com.moni.trade.holding.domain.entity;

import com.moni.trade.global.entity.TradeBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "holding")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holding extends TradeBaseEntity {

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID accountId;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal averagePrice;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    public static Holding create(UUID accountId, String ticker, Integer quantity, BigDecimal price) {
        Holding holding = new Holding();
        holding.accountId = accountId;
        holding.ticker = ticker;
        holding.quantity = quantity;
        holding.averagePrice = price;
        holding.totalAmount = price.multiply(BigDecimal.valueOf(quantity));
        return holding;
    }

    public void buy(Integer additionalQuantity, BigDecimal price) {
        BigDecimal newTotalAmount = this.totalAmount.add(price.multiply(BigDecimal.valueOf(additionalQuantity)));
        int newQuantity = this.quantity + additionalQuantity;
        this.averagePrice = newTotalAmount.divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);
        this.totalAmount = newTotalAmount;
        this.quantity = newQuantity;
    }

    public void sell(Integer sellQuantity) {
        this.quantity -= sellQuantity;
        this.totalAmount = this.averagePrice.multiply(BigDecimal.valueOf(this.quantity));
    }
}
