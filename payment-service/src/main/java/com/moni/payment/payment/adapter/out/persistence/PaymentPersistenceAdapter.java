package com.moni.payment.payment.adapter.out.persistence;

import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentHistory;
import com.moni.payment.domain.port.LoadPaymentPort;
import com.moni.payment.domain.port.SavePaymentHistoryPort;
import com.moni.payment.domain.port.SavePaymentPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentPersistenceAdapter implements SavePaymentPort, LoadPaymentPort, SavePaymentHistoryPort {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentHistoryJpaRepository paymentHistoryJpaRepository;

    public PaymentPersistenceAdapter(
            PaymentJpaRepository paymentJpaRepository,
            PaymentHistoryJpaRepository paymentHistoryJpaRepository) {
        this.paymentJpaRepository = paymentJpaRepository;
        this.paymentHistoryJpaRepository = paymentHistoryJpaRepository;
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = PaymentJpaEntity.fromDomain(payment);
        return paymentJpaRepository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(UUID id) {
        return paymentJpaRepository.findById(id)
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByMerchantId(MerchantId merchantId) {
        return paymentJpaRepository.findByMerchantId(merchantId.getValue())
                .map(PaymentJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByUserId(UUID userId, int page, int size) {
        return paymentJpaRepository.findByUserIdPaged(userId, PageRequest.of(page, size))
                .stream()
                .map(PaymentJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void save(PaymentHistory history) {
        paymentJpaRepository.findById(history.getPaymentId())
                .ifPresent(paymentEntity -> {
                    PaymentHistoryJpaEntity historyEntity =
                            PaymentHistoryJpaEntity.fromDomain(history, paymentEntity);
                    paymentHistoryJpaRepository.save(historyEntity);
                });
    }
}
