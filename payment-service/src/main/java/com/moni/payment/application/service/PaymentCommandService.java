package com.moni.payment.application.service;

import com.moni.payment.application.command.ApprovePaymentCommand;
import com.moni.payment.application.command.FailPaymentCommand;
import com.moni.payment.application.command.RecordPendingPaymentCommand;
import com.moni.payment.common.exception.PaymentErrorCode;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.infrastructure.repository.PaymentHistoryRepository;
import com.moni.payment.infrastructure.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID recordPendingPayment(RecordPendingPaymentCommand command) {
        Payment payment = Payment.create(
                command.userId(), command.merchantId(), command.amount(),
                command.paymentType(), command.expiresAt(), command.actor());
        paymentJpaRepository.save(payment);
        log.info("결제 PENDING 저장: paymentId={}, userId={}", payment.getId(), command.userId());
        return payment.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void approvePayment(ApprovePaymentCommand command) {
        Payment payment = loadPayment(command.paymentId());
        payment.complete(command.pgPaymentKey(), command.billingKeyValue(),
                command.rawResponse(), Instant.now(), command.actor());
        paymentJpaRepository.save(payment);
        paymentHistoryRepository.save(payment.pullLatestHistory());
        payment.pullDomainEvents().forEach(applicationEventPublisher::publishEvent);
        log.info("결제 COMPLETED 저장: paymentId={}", command.paymentId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failPayment(FailPaymentCommand command) {
        Payment payment = loadPayment(command.paymentId());
        payment.fail(command.pgResponse(), Instant.now(), command.actor());
        paymentJpaRepository.save(payment);
        paymentHistoryRepository.save(payment.pullLatestHistory());
        log.warn("결제 FAILED 저장: paymentId={}", command.paymentId());
    }

    private Payment loadPayment(UUID paymentId) {
        return paymentJpaRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }
}
