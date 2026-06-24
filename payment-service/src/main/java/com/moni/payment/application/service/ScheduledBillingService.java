package com.moni.payment.application.service;

import com.moni.payment.application.repository.PaymentRepository;
import com.moni.payment.application.repository.PgPaymentClient;
import com.moni.payment.application.repository.SubscriptionEventPublisher;
import com.moni.payment.application.repository.SubscriptionRepository;
import com.moni.payment.application.usecase.ProcessScheduledBillingUseCase;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.BillingFailedEvent;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.domain.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledBillingService implements ProcessScheduledBillingUseCase {

    static final Money SUBSCRIPTION_AMOUNT = Money.of(9900L);

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PgPaymentClient pgPaymentClient;
    private final SubscriptionEventPublisher subscriptionEventPublisher;

    @Override
    @Transactional
    public void processScheduledBilling() {
        LocalDate today = LocalDate.now();
        List<Subscription> dueSubscriptions = subscriptionRepository.findActiveSubscriptionsDueBefore(today);
        log.info("정기결제 대상 구독 수: {}", dueSubscriptions.size());

        for (Subscription subscription : dueSubscriptions) {
            processOneBilling(subscription, today);
        }
    }

    private void processOneBilling(Subscription subscription, LocalDate today) {
        MerchantId merchantId = MerchantId.generate();

        try {
            PgPaymentClient.PgPaymentResult result = pgPaymentClient.requestBillingPayment(
                    subscription.getBillingKey().getValue(), SUBSCRIPTION_AMOUNT, merchantId);

            if (result.success()) {
                Payment payment = Payment.create(
                        subscription.getUserId(), merchantId, SUBSCRIPTION_AMOUNT,
                        PaymentType.SUBSCRIPTION_RECURRING,
                        Instant.now().plusSeconds(60),
                        "SYSTEM");
                payment.complete(result.pgPaymentKey(), null, result.rawResponse(), Instant.now(), "SYSTEM");
                paymentRepository.save(payment);
                paymentRepository.saveHistory(payment.pullLatestHistory());

                subscription.extendBillingDate(today.plusMonths(1));
                subscriptionRepository.save(subscription);

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
        subscriptionRepository.save(subscription);
        subscription.getHistories().forEach(subscriptionRepository::saveHistory);

        BillingFailedEvent event = new BillingFailedEvent(
                subscription.getId(), subscription.getUserId(), SUBSCRIPTION_AMOUNT, reason);
        subscriptionEventPublisher.publishBillingFailed(event);

        log.warn("정기결제 실패, 구독 정지: subscriptionId={}", subscription.getId());
    }
}
