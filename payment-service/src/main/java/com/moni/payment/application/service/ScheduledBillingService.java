package com.moni.payment.application.service;

import com.moni.payment.application.repository.PaymentRepository;
import com.moni.payment.application.repository.PgGateway;
import com.moni.payment.application.repository.SubscriptionEventPublisher;
import com.moni.payment.application.repository.SubscriptionRepository;
import com.moni.payment.application.usecase.ProcessScheduledBillingUseCase;
import com.moni.payment.common.exception.PaymentException;
import com.moni.payment.domain.event.BillingFailedEvent;
import com.moni.payment.domain.model.MerchantId;
import com.moni.payment.domain.model.Money;
import com.moni.payment.domain.model.Payment;
import com.moni.payment.domain.model.PaymentHistory;
import com.moni.payment.domain.model.PaymentType;
import com.moni.payment.domain.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledBillingService implements ProcessScheduledBillingUseCase {

    static final Money SUBSCRIPTION_AMOUNT = Money.of(9900L);

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final PgGateway pgGateway;
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
        MerchantId merchantId = MerchantId.of("MONI" + UUID.randomUUID().toString().replace("-", ""));

        try {
            PgGateway.PgPaymentResult result = pgGateway.requestBillingPayment(
                    subscription.getBillingKey().getValue(), SUBSCRIPTION_AMOUNT, merchantId);

            if (result.success()) {
                Payment payment = Payment.initiate(
                        subscription.getUserId(), merchantId, SUBSCRIPTION_AMOUNT,
                        PaymentType.SUBSCRIPTION_RECURRING,
                        Instant.now().plusSeconds(60),
                        "SYSTEM");
                payment.complete(result.pgPaymentKey(), result.rawResponse(), Instant.now(), "SYSTEM");
                paymentRepository.save(payment);
                List<PaymentHistory> histories = payment.getHistories();
                paymentRepository.saveHistory(histories.get(histories.size() - 1));

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
