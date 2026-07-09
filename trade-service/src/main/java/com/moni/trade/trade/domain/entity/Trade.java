package com.moni.trade.trade.domain.entity;

import com.moni.trade.global.entity.TradeBaseEntity;
import com.moni.trade.trade.domain.enums.TradeStatus;
import com.moni.trade.trade.domain.enums.TradeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "p_trades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade extends TradeBaseEntity {

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID accountId;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private TradeType tradeType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 18, scale = 2)
    private BigDecimal profitAmount;

    @Column(precision = 8, scale = 4)
    private BigDecimal profitRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeStatus status;

    public static Trade createBuy(UUID accountId, String ticker, BigDecimal quantity, BigDecimal price) {
        Trade trade = new Trade();
        trade.accountId = accountId;
        trade.ticker = ticker;
        trade.tradeType = TradeType.BUY;
        trade.quantity = quantity;
        trade.price = price;
        trade.totalAmount = price.multiply(quantity).setScale(2, RoundingMode.DOWN);
        trade.status = TradeStatus.PENDING;
        return trade;
    }

    public static Trade createSell(UUID accountId, String ticker, BigDecimal quantity, BigDecimal price,
                                   BigDecimal profitAmount, BigDecimal profitRate) {
        Trade trade = new Trade();
        trade.accountId = accountId;
        trade.ticker = ticker;
        trade.tradeType = TradeType.SELL;
        trade.quantity = quantity;
        trade.price = price;
        trade.totalAmount = price.multiply(quantity).setScale(2, RoundingMode.DOWN);
        trade.profitAmount = profitAmount;
        trade.profitRate = profitRate;
        trade.status = TradeStatus.PENDING;
        return trade;
    }

    public void complete() {
        this.status = TradeStatus.DONE;
    }

    public void fail() {
        this.status = TradeStatus.FAILED;
    }
}
