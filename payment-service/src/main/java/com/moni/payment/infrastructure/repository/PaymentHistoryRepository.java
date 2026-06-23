package com.moni.payment.infrastructure.repository;

import com.moni.payment.domain.model.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {

    List<PaymentHistory> findByPaymentIdOrderByRequestedAtAsc(UUID paymentId);
}
