package com.moni.payment.infrastructure.persistence;

import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentHistory;
import com.moni.payment.domain.port.LoadPaymentPort;
import com.moni.payment.domain.port.SavePaymentHistoryPort;
import com.moni.payment.domain.port.SavePaymentPort;
import com.moni.payment.infrastructure.repository.PaymentHistoryRepository;
import com.moni.payment.infrastructure.repository.PaymentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentPersistenceAdapter implements SavePaymentPort, LoadPaymentPort, SavePaymentHistoryPort {

    private final PaymentRepository paymentRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    public PaymentPersistenceAdapter(
            PaymentRepository paymentRepository,
            PaymentHistoryRepository paymentHistoryRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentHistoryRepository = paymentHistoryRepository;
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findById(UUID id) {
        return paymentRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Payment> findByMerchantId(MerchantId merchantId) {
        return paymentRepository.findByMerchantId(merchantId.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> findByUserId(UUID userId, int page, int size) {
        return paymentRepository.findByUserIdPaged(userId, PageRequest.of(page, size));
    }

    @Override
    @Transactional
    public void save(PaymentHistory history) {
        paymentHistoryRepository.save(history);
    }
}
