package com.moni.payment.subscription.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubscriptionHistoryJpaRepository extends JpaRepository<SubscriptionHistoryJpaEntity, UUID> {
}
