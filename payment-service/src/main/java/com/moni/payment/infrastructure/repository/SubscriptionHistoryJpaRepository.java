package com.moni.payment.infrastructure.repository;

import com.moni.payment.infrastructure.persistence.SubscriptionHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubscriptionHistoryJpaRepository extends JpaRepository<SubscriptionHistoryJpaEntity, UUID> {
}
