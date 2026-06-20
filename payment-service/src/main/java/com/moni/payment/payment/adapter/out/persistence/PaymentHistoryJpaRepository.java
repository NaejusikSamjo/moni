package com.moni.payment.payment.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentHistoryJpaRepository extends JpaRepository<PaymentHistoryJpaEntity, UUID> {
}
