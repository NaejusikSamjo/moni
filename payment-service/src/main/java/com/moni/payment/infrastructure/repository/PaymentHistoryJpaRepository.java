package com.moni.payment.infrastructure.repository;

import com.moni.payment.infrastructure.persistence.PaymentHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentHistoryJpaRepository extends JpaRepository<PaymentHistoryJpaEntity, UUID> {
}
