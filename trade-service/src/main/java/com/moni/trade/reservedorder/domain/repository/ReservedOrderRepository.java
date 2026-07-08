package com.moni.trade.reservedorder.domain.repository;

import com.moni.trade.reservedorder.domain.entity.ReservedOrder;
import com.moni.trade.reservedorder.domain.enums.ReservedOrderStatus;
import com.moni.trade.reservedorder.domain.enums.ReservedOrderType;
import com.moni.trade.trade.domain.enums.TradeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservedOrderRepository extends JpaRepository<ReservedOrder, UUID> {

    List<ReservedOrder> findByOrderTypeAndStatusOrderByCreatedAtAsc(
            ReservedOrderType orderType, ReservedOrderStatus status);

    List<ReservedOrder> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    Optional<ReservedOrder> findByIdAndAccountId(UUID id, UUID accountId);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM ReservedOrder r "
            + "WHERE r.accountId = :accountId AND r.ticker = :ticker "
            + "AND r.tradeType = :tradeType AND r.status = :status")
    BigDecimal sumQuantityByAccountIdAndTickerAndTradeTypeAndStatus(
            @Param("accountId") UUID accountId,
            @Param("ticker") String ticker,
            @Param("tradeType") TradeType tradeType,
            @Param("status") ReservedOrderStatus status);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM ReservedOrder r "
            + "WHERE r.accountId = :accountId AND r.tradeType = :tradeType AND r.status = :status")
    BigDecimal sumAmountByAccountIdAndTradeTypeAndStatus(
            @Param("accountId") UUID accountId,
            @Param("tradeType") TradeType tradeType,
            @Param("status") ReservedOrderStatus status);
}
