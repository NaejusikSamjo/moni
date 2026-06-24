package com.moni.payment.application.service;

import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.BillingFailedEvent;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.domain.model.Subscription;
import com.moni.payment.domain.model.SubscriptionStatus;
import com.moni.payment.infrastructure.client.toss.TossPaymentsAdapter;
import com.moni.payment.infrastructure.repository.PaymentHistoryRepository;
import com.moni.payment.infrastructure.repository.PaymentJpaRepository;
import com.moni.payment.infrastructure.repository.SubscriptionHistoryRepository;
import com.moni.payment.infrastructure.repository.SubscriptionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledBillingService {

    static final Money SUBSCRIPTION_AMOUNT = Money.of(9900L);

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final SubscriptionHistoryRepository subscriptionHistoryRepository;
    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final TossPaymentsAdapter tossPaymentsAdapter;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public void processScheduledBilling() {
        LocalDate today = LocalDate.now();
        List<Subscription> dueSubscriptions =
                subscriptionJpaRepository.findActiveSubscriptionsDueBefore(SubscriptionStatus.ACTIVE, today);
        log.info("정기결제 대상 구독 수: {}", dueSubscriptions.size());

        for (Subscription subscription : dueSubscriptions) {
            processOneBilling(subscription, today);
        }
    }

    private void processOneBilling(Subscription subscription, LocalDate today) {
        MerchantId merchantId = MerchantId.generate();

        try {
            TossPaymentsAdapter.PgPaymentResult result = tossPaymentsAdapter.requestBillingPayment(
                    subscription.getBillingKey().getValue(), SUBSCRIPTION_AMOUNT, merchantId);

            if (result.success()) {
                Payment payment = Payment.create(
                        subscription.getUserId(), merchantId, SUBSCRIPTION_AMOUNT,
                        PaymentType.SUBSCRIPTION_RECURRING,
                        Instant.now().plusSeconds(60),
                        "SYSTEM");
                payment.complete(result.pgPaymentKey(), null, result.rawResponse(), Instant.now(), "SYSTEM");
                paymentJpaRepository.save(payment);
                paymentHistoryRepository.save(payment.pullLatestHistory());

                subscription.extendBillingDate(today.plusMonths(1));
                subscriptionJpaRepository.save(subscription);

                log.info("정기결제 성공: subscriptionId={}, nextBillingDate={}",
                        subscription.getId(), today.plusMonths(1));
            } else {
                handleBillingFailure(subscription, result.rawResponse());
            }
        } catch (PaymentException e) {
            handleBillingFailure(subscription, e.getMessage());
        }
    }

    private void handleBillingFailure(Subscription subscription, String reason) {
        subscription.suspend(reason);
        subscriptionJpaRepository.save(subscription);
        subscription.getHistories().forEach(subscriptionHistoryRepository::save);

        BillingFailedEvent event = new BillingFailedEvent(
                subscription.getId(), subscription.getUserId(), SUBSCRIPTION_AMOUNT, reason);
        applicationEventPublisher.publishEvent(event);

        log.warn("정기결제 실패, 구독 정지: subscriptionId={}", subscription.getId());
    }
}
