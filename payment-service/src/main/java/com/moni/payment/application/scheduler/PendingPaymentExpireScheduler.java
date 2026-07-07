package com.moni.payment.application.scheduler;

import com.moni.payment.application.dto.command.FailPaymentCommand;
import com.moni.payment.application.service.command.PaymentCommandService;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentStatus;
import com.moni.payment.infrastructure.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingPaymentExpireScheduler {

    private static final String ACTOR = "system";
    private static final String EXPIRE_REASON = "PAYMENT_EXPIRED";

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentCommandService paymentCommandService;

    @Scheduled(cron = "0 * * * * *")
    public void expireOverduePayments() {
        List<Payment> expired = paymentJpaRepository
                .findByStatusAndExpiresAtBefore(PaymentStatus.PENDING, Instant.now());

        log.info("만료 PENDING 결제 처리 대상: {}건", expired.size());

        for (Payment payment : expired) {
            try {
                paymentCommandService.failPayment(
                        new FailPaymentCommand(payment.getId(), EXPIRE_REASON, ACTOR));
                log.info("만료 결제 FAILED 전환 완료: paymentId={}", payment.getId());
            } catch (Exception e) {
                log.error("만료 결제 FAILED 전환 실패: paymentId={}", payment.getId(), e);
            }
        }
    }
}
