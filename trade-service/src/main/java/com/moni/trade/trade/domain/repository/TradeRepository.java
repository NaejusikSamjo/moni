package com.moni.trade.trade.domain.repository;

import com.moni.trade.trade.domain.entity.Trade;
import com.moni.trade.trade.domain.enums.TradeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {

    Page<Trade> findByAccountId(UUID accountId, Pageable pageable);

    Page<Trade> findByAccountIdAndTradeType(UUID accountId, TradeType tradeType, Pageable pageable);
}
