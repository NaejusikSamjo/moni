package com.moni.trade.holding.domain.entity;

import com.moni.trade.global.entity.TradeBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal averagePrice;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Version
    private Long version;

    public static Holding create(UUID accountId, String ticker, BigDecimal quantity, BigDecimal price) {
        Holding holding = new Holding();
        holding.accountId = accountId;
        holding.ticker = ticker;
        holding.quantity = quantity;
        holding.averagePrice = price;
        holding.totalAmount = price.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        return holding;
    }

    public void buy(BigDecimal additionalQuantity, BigDecimal price) {
        BigDecimal newTotalAmount = this.totalAmount.add(price.multiply(additionalQuantity));
        BigDecimal newQuantity = this.quantity.add(additionalQuantity);
        this.averagePrice = newTotalAmount.divide(newQuantity, 2, RoundingMode.HALF_UP);
        this.totalAmount = newTotalAmount;
        this.quantity = newQuantity;
    }

    public void sell(BigDecimal sellQuantity) {
        this.quantity = this.quantity.subtract(sellQuantity);
        this.totalAmount = this.averagePrice.multiply(this.quantity).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isEmpty() {
        return this.quantity.compareTo(BigDecimal.ZERO) <= 0;
    }
}
