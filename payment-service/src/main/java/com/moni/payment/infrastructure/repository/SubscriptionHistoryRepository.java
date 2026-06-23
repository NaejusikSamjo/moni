package com.moni.payment.infrastructure.repository;

import com.moni.payment.domain.model.SubscriptionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, UUID> {
}
