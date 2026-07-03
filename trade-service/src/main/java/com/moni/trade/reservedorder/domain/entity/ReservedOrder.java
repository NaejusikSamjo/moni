package com.moni.trade.reservedorder.domain.entity;

import com.moni.trade.global.entity.TradeBaseEntity;
import com.moni.trade.reservedorder.domain.enums.ReservedOrderStatus;
import com.moni.trade.reservedorder.domain.enums.ReservedOrderType;
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
import java.util.UUID;

@Entity
@Table(name = "reserved_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservedOrder extends TradeBaseEntity {

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID accountId;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ReservedOrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private TradeType tradeType;

    @Column(precision = 18, scale = 2)
    private BigDecimal targetPrice;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(precision = 18, scale = 2)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReservedOrderStatus status;

    public static ReservedOrder createLimitBuy(UUID accountId, String ticker,
                                               BigDecimal targetPrice, BigDecimal amount) {
        ReservedOrder order = new ReservedOrder();
        order.accountId = accountId;
        order.ticker = ticker;
        order.orderType = ReservedOrderType.LIMIT;
        order.tradeType = TradeType.BUY;
        order.targetPrice = targetPrice;
        order.amount = amount;
        order.status = ReservedOrderStatus.PENDING;
        return order;
    }

    public static ReservedOrder createLimitSell(UUID accountId, String ticker,
                                                BigDecimal targetPrice, BigDecimal quantity) {
        ReservedOrder order = new ReservedOrder();
        order.accountId = accountId;
        order.ticker = ticker;
        order.orderType = ReservedOrderType.LIMIT;
        order.tradeType = TradeType.SELL;
        order.targetPrice = targetPrice;
        order.quantity = quantity;
        order.status = ReservedOrderStatus.PENDING;
        return order;
    }

    public static ReservedOrder createReservationBuy(UUID accountId, String ticker, BigDecimal amount) {
        ReservedOrder order = new ReservedOrder();
        order.accountId = accountId;
        order.ticker = ticker;
        order.orderType = ReservedOrderType.RESERVATION;
        order.tradeType = TradeType.BUY;
        order.amount = amount;
        order.status = ReservedOrderStatus.PENDING;
        return order;
    }

    public static ReservedOrder createReservationSell(UUID accountId, String ticker, BigDecimal quantity) {
        ReservedOrder order = new ReservedOrder();
        order.accountId = accountId;
        order.ticker = ticker;
        order.orderType = ReservedOrderType.RESERVATION;
        order.tradeType = TradeType.SELL;
        order.quantity = quantity;
        order.status = ReservedOrderStatus.PENDING;
        return order;
    }

    public void execute() {
        this.status = ReservedOrderStatus.EXECUTED;
    }

    public void cancel() {
        this.status = ReservedOrderStatus.CANCELLED;
    }

    public void fail() {
        this.status = ReservedOrderStatus.FAILED;
    }

    public boolean isLimitConditionMet(BigDecimal currentPrice) {
        if (tradeType == TradeType.BUY) {
            return currentPrice.compareTo(targetPrice) <= 0;
        }
        return currentPrice.compareTo(targetPrice) >= 0;
    }
}
