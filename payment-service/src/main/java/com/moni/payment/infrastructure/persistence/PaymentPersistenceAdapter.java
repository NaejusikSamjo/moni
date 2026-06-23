package com.moni.payment.infrastructure.persistence;

import com.moni.payment.application.repository.PaymentRepository;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentHistory;
import com.moni.payment.infrastructure.repository.PaymentHistoryRepository;
import com.moni.payment.infrastructure.repository.PaymentJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentPersistenceAdapter implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    public PaymentPersistenceAdapter(
            PaymentJpaRepository paymentJpaRepository,
            PaymentHistoryRepository paymentHistoryRepository) {
        this.paymentJpaRepository = paymentJpaRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
    }

    @Override
    public Payment save(Payment payment) {
        return paymentJpaRepository.save(payment);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return paymentJpaRepository.findById(id);
    }

    @Override
    public Optional<Payment> findByMerchantId(MerchantId merchantId) {
        return paymentJpaRepository.findByMerchantId(merchantId.getValue());
    }

    @Override
    public List<Payment> findByUserId(UUID userId, int page, int size) {
        return paymentJpaRepository.findByUserIdPaged(userId, PageRequest.of(page, size));
    }

    @Override
    public void saveHistory(PaymentHistory history) {
        paymentHistoryRepository.save(history);
    }
}
