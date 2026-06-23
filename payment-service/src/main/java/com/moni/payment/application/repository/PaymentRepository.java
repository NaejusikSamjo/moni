package com.moni.payment.application.repository;

import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentHistory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByMerchantId(MerchantId merchantId);

    List<Payment> findByUserId(UUID userId, int page, int size);

    void saveHistory(PaymentHistory history);
}
