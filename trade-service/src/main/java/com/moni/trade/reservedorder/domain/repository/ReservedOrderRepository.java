package com.moni.trade.reservedorder.domain.repository;

import com.moni.trade.reservedorder.domain.entity.ReservedOrder;
import com.moni.trade.reservedorder.domain.enums.ReservedOrderStatus;
import com.moni.trade.reservedorder.domain.enums.ReservedOrderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservedOrderRepository extends JpaRepository<ReservedOrder, UUID> {

    List<ReservedOrder> findByOrderTypeAndStatusOrderByCreatedAtAsc(
            ReservedOrderType orderType, ReservedOrderStatus status);

    List<ReservedOrder> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    Optional<ReservedOrder> findByIdAndAccountId(UUID id, UUID accountId);
}
